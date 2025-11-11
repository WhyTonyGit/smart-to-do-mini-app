package com.smarttodo.app.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttodo.app.llm.dto.ParseResult;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
                "stream", false,
                "format", "json",
                "options", Map.of(
                        "temperature", 0.0,
                        "top_p", 0.9,
                        "seed", 42,
                        "num_predict", 300
                ),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user",   "content", userPrompt)
                )
        );

        return client.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(props.timeoutSeconds()))
                .flatMap(raw -> {
                    try {
                        JsonNode root = om.readTree(raw);
                        String content = root.path("message").path("content").asText("");
                        if (content == null || content.isBlank()) {
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
