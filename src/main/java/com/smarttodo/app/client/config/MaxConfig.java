package com.smarttodo.app.client.config;

import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(MaxProps.class)
public class MaxConfig {

    @Bean
    public WebClient maxClient(MaxProps p) {
        ConnectionProvider pool = ConnectionProvider.builder("max-pool")
                .maxConnections(100)
                .pendingAcquireMaxCount(1000)
                .build();

        HttpClient httpClient = HttpClient.create(pool)
                .responseTimeout(Duration.ofSeconds(5));

        //Настройка размера буффера
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
                .build();

        return WebClient.builder()
                .baseUrl(p.baseUrl())
                .defaultHeaders(h -> h.set(HttpHeaders.AUTHORIZATION, p.token()))
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .exchangeStrategies(strategies)
                .build();
    }
    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            StringBuilder sb = new StringBuilder();
            sb.append("OUTBOUND ").append(req.method()).append(" ").append(req.url()).append('\n');
            req.headers().forEach((n, v) -> sb.append("  ").append(n).append(": ").append(v).append('\n'));
            return Mono.just(req)
                    .flatMap(r -> r.body() == null ? Mono.just(req) : Mono.just(req)) // тело логируем ниже
                    .doOnSuccess(x -> LoggerFactory.getLogger("MaxApi").info(sb.toString()));
        });
    }

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> {
            var log = LoggerFactory.getLogger("MaxApi");
            log.info("OUTBOUND <== {} {}", resp.statusCode().value(), resp.statusCode());
            // Снимем и тело ошибки (важно при 400)
            return resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> {
                        if (!body.isEmpty()) log.info("OUTBOUND response body:\n{}", body);
                        // ВОССТАНАВЛИВАЕМ тело, чтобы дальше его можно было заново прочитать
                        return Mono.just(ClientResponse.create(resp.statusCode())
                                .headers(h -> h.addAll(resp.headers().asHttpHeaders()))
                                .body(body)
                                .build());
                    });
        });
    }
    @Bean
    public org.springframework.boot.CommandLineRunner sanity(MaxProps p) {
        return args -> {
            if (p.token() == null || p.token().isBlank())
                throw new IllegalStateException("MAX_BOT_TOKEN env is required");
            if (p.baseUrl() == null || p.baseUrl().isBlank())
                throw new IllegalStateException("max.api.base-url must be set");
        };
    }
}
