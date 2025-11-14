package com.smarttodo.app.llm.motivation;

import com.smarttodo.app.llm.OllamaClient;
import com.smarttodo.app.llm.motivation.dto.MotivationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MotivationService {
    private final OllamaClient ollama;

    public Mono<MotivationResponse> generateMotivation(int streakDays) {
        String system = String.format("Перепиши это своими словами: Ты молодец, держишься уже $d, продолжай в том же духе", streakDays);
        String user = system;

        return ollama.chatText(system, user)
                .map(text -> new MotivationResponse(cleanMessage(text)));
    }

    private String cleanMessage(String raw) {
        var msg = raw.trim();
        if (msg.startsWith("\"") && msg.endsWith("\"") && msg.length() > 1) {
            msg = msg.substring(1, msg.length() - 1).trim();
        }
        return msg;
    }
}
