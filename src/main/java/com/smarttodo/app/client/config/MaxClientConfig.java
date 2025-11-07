package com.smarttodo.app.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class MaxClientConfig {
    @Bean
    WebClient maxWebClient(
            @Value("${max.api-base-url}") String baseUrl,
            @Value("${max.token}") String token
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", token)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            System.out.println(">> " + req.method() + " " + req.url());
            return reactor.core.publisher.Mono.just(req);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(res -> {
            System.out.println("<< " + res.statusCode());
            return reactor.core.publisher.Mono.just(res);
        });
    }
}
