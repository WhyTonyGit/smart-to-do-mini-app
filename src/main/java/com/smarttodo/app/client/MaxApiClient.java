package com.smarttodo.app.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.smarttodo.app.dto.MeResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class MaxApiClient {

    private final WebClient web;

    public MaxApiClient(WebClient maxWebClient) {
        this.web = maxWebClient;
    }

    // Типизированный пример
    public MeResponse getMe() {
        return web.get()
                .uri("/me")
                .retrieve()
                .bodyToMono(MeResponse.class)
                .block();
    }

    // Универсальные методы — удобно, когда нет точных DTO
    public JsonNode get(String path) {
        return web.get()
                .uri(path)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    public JsonNode post(String path, Object body) {
        return web.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    public JsonNode patch(String path, Object body) {
        return web.patch()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    public JsonNode delete(String path) {
        return web.delete()
                .uri(path)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    /* Пример конкретной операции отправки сообщения.
       Схему тела проверьте в своей доке — ниже типичный шаблон.
    */
    public JsonNode sendMessage(long chatId, String text) {
        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", text
        );
        return post("/messages", body);
    }

    public JsonNode getMessage(long messageId) {
        return get("/messages/" + messageId);
    }
}