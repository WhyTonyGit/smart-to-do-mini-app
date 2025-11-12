package com.smarttodo.app.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttodo.app.llm.dto.ParseResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OllamaClient {
    private final WebClient client;
    private final OllamaProps props;
    private final ObjectMapper om;

    public OllamaClient(
            @Qualifier("ollamaWebClient") WebClient client,
            OllamaProps props,
            ObjectMapper om
    ) {
        this.client = client;
        this.props = props;
        this.om = om;
    }

    public Mono<ParseResult> chatExtractJson(String systemPrompt, String userPrompt) {
        var body = Map.of(
                "model", props.model(),
                "stream", true,
                "format", "json",
                "keep_alive", "10m",
                "options", Map.of(
                        "temperature", 0.0,
                        "top_p", 0.9,
                        "seed", 42,
                        "num_predict", 128,
                        "stop", List.of("```", "<|im_end|>")
                ),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user",   "content", userPrompt)
                )
        );

        StringBuilder acc = new StringBuilder();

        return client.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON)   // ollama стримит NDJSON
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(props.timeoutSeconds()))
                .map(chunk -> {
                    // каждый chunk — JSON-объект с полями { "message": { "content": "..." }, "done": false }
                    try {
                        JsonNode n = om.readTree(chunk);
                        if (n.path("message").has("content")) {
                            acc.append(n.path("message").path("content").asText(""));
                        }
                        return n.path("done").asBoolean(false);
                    } catch (Exception e) {
                        // игнорим мусорные чанки/пустые keep-alive строки
                        return false;
                    }
                })
                .filter(done -> done)                  // ждём финальный чанк
                .next()
                .flatMap(done -> {
                    try {
                        // format:"json" даёт JSON-текст в acc — парсим в твою модель
                        var content = acc.toString().trim();
                        if (content.isBlank()) {
                            return Mono.error(new IllegalStateException("empty LLM content"));
                        }
                        ParseResult pr = om.readValue(content, ParseResult.class);
                        return Mono.just(pr);
                    } catch (Exception e) {
                        return Mono.error(new IllegalStateException("Failed to parse LLM JSON: " + e.getMessage(), e));
                    }
                });
        }
}
