package com.smarttodo.app.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class MaxApi {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Retry RETRY_5XX_OR_NETWORK = Retry
            .backoff(3, Duration.ofMillis(300))
            .filter(MaxApi::isTransient);

    private final WebClient client; // собран в MaxConfig: baseUrl + Authorization

    public MaxApi(@Qualifier("maxClient") WebClient client) {
        this.client = client;
    }

    /** Отправка простого текста в чат (MAX: POST /messages?chat_id=...) */
    public void sendText(long chatId, String text) {
        if (chatId <= 0) throw new IllegalArgumentException("chatId must be > 0");
        if (text == null || text.isBlank()) return; // молча игнорим пустяки

        Map<String, Object> body = Map.of("text", text);

        postMessage(chatId, body)
                .timeout(TIMEOUT)
                .retryWhen(RETRY_5XX_OR_NETWORK)
                .block(); // вебхук-обработчик синхронный: дожидаемся
    }

    public void sendStartKeyboard(long chatId) {
        var body = Map.of(
                "text", """
                        Привет! Я твой помощник по самоорганизации. Помогу планировать день, вести задачи, отслеживать часы активности и формировать полезные привычки.
                        
                        Что я делаю:
                        • Быстро добавляю и напоминаю о задачах
                        • Следую за прогрессом и показываю статистику выполнения
                        • Замеряю «часы активности» — когда ты реально делаешь дела
                        • Запускаю трекеры привычек и мотивирую не срываться
                        
                        Готов начать?
                        """,
                "attachments", List.of(
                        Map.of(
                                "type", "inline_keyboard",
                                "payload", Map.of(
                                        "buttons", List.of(
                                                List.of(
                                                        Map.of(
                                                                "type", "callback",
                                                                "text", "✅Задачи",
                                                                "payload", "tasks-handler"
                                                        ),
                                                        Map.of(
                                                                "type", "callback",
                                                                "text", "🗓️Привычки",
                                                                "payload", "habit-handler"
                                                        ),
                                                        Map.of(
                                                                "type", "callback",
                                                                "text", "⏰Напоминания",
                                                                "payload", "notification-handler"
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        postMessage(chatId, body)             // твой внутренний метод
                .timeout(TIMEOUT)
                .retryWhen(RETRY_5XX_OR_NETWORK)
                .block();
    }

    public void sendTaskKeyboard(long chatId) {
        var body = Map.of(
                "text", """
                        📝**Меню задач**
                        """,
                "attachments", List.of(
                        Map.of(
                                "type", "inline_keyboard",
                                "payload", Map.of(
                                        "buttons", List.of(
                                                List.of(
                                                        Map.of(
                                                                "type", "callback",
                                                                "text", "Задачи на сегодня",
                                                                "payload", "tasks-get-today"
                                                        ),
                                                        Map.of(
                                                                "type", "callback",
                                                                "text", "Задачи на неделю",
                                                                "payload", "tasks-get-week"
                                                        ),
                                                        Map.of(
                                                                "type", "callback",
                                                                "text", "Создать задачу",
                                                                "payload", "tasks-create-new"
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                "format", "markdown"
        );

        postMessage(chatId, body)             // твой внутренний метод
                .timeout(TIMEOUT)
                .retryWhen(RETRY_5XX_OR_NETWORK)
                .block();
    }

    public void sendOpenLink(long chatId, String url, String title) {
        var body = Map.of(
                "text", "Открой мини-приложение",
                "attachments", java.util.List.of(
                        Map.of(
                                "type", "inline_keyboard",
                                "payload", Map.of(
                                        "buttons", java.util.List.of(
                                                java.util.List.of( // один ряд
                                                        Map.of(
                                                                "type", "link",
                                                                "text", title != null ? title : "Открыть",
                                                                "url", url
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        postMessage(chatId, body)
                .timeout(TIMEOUT)
                .retryWhen(RETRY_5XX_OR_NETWORK)
                .block();
    }

    /** MAX принимает получателя только в query; в теле — контент (NewMessageBody). */
    private Mono<ResponseEntity<Void>> postMessage(long chatId, Object body) {
        return client.post()
                .uri(b -> b.path("/messages").queryParam("chat_id", chatId).build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        s -> s.is4xxClientError(),
                        resp -> resp.bodyToMono(String.class).defaultIfEmpty("")
                                .map(err -> new MaxClientException(
                                        "4xx from MAX: " + resp.statusCode().value() + " body=" + err))
                )
                .onStatus(
                        s -> s.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class).defaultIfEmpty("")
                                .map(err -> new MaxServerException(
                                        "5xx from MAX: " + resp.statusCode().value() + " body=" + err))
                )
                .toBodilessEntity();
    }

    private static boolean isTransient(Throwable t) {
        if (t instanceof MaxServerException) return true; // 5xx
        String n = t.getClass().getName();
        return n.contains("Timeout") || n.contains("Connect") || n.contains("IOException");
    }

    public static class MaxClientException extends RuntimeException {
        public MaxClientException(String msg) { super(msg); }
    }

    public static class MaxServerException extends RuntimeException {
        public MaxServerException(String msg) { super(msg); }
    }
}
