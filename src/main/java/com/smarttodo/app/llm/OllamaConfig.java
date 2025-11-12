package com.smarttodo.app.llm;

import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(OllamaProps.class)
public class OllamaConfig {

    @Bean(name = "ollamaWebClient")
    public WebClient ollamaClient(OllamaProps p) {
        var pool = ConnectionProvider.builder("ollama-pool")
                .maxConnections(50)
                .pendingAcquireMaxCount(500)
                .build();

        var http = HttpClient.create(pool)
                .responseTimeout(Duration.ofSeconds(p.timeoutSeconds()));

        var strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(p.baseUrl())
                .defaultHeaders(h -> {
                    h.set(HttpHeaders.ACCEPT, "application/json");
                })
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(http))
                .exchangeStrategies(strategies)
                .filter(logReq())
                .filter(logResp())
                .build();
    }

    private static ExchangeFilterFunction logReq() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            var sb = new StringBuilder("OLLAMA ==> ").append(req.method()).append(" ").append(req.url()).append('\n');
            req.headers().forEach((n, v) -> sb.append("  ").append(n).append(": ").append(v).append('\n'));
            LoggerFactory.getLogger("OllamaApi").info(sb.toString());
            return Mono.just(req);
        });
    }

    private static ExchangeFilterFunction logResp() {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> resp.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    var log = LoggerFactory.getLogger("OllamaApi");
                    log.info("OLLAMA <== {} {}\n{}", resp.statusCode().value(), resp.statusCode(), body);
                    return Mono.just(org.springframework.web.reactive.function.client.ClientResponse.create(resp.statusCode())
                            .headers(h -> h.addAll(resp.headers().asHttpHeaders()))
                            .body(body).build());
                }));
    }
}
