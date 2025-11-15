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

    private static final String SYSTEM_PROMPT = "Rephrase the user sentence in Russian. Keep the same meaning. " +
            "Do not copy the sentence, change the words and word order. " +
            "Answer with only the new Russian sentence.";

    public Mono<MotivationResponse> generateMotivation(int streakDays) {
        String user = String.format("Ты молодец, держишься уже %d дней, продолжай в том же духе.", streakDays);

        return ollama.chatText(SYSTEM_PROMPT, user)
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
