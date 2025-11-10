package com.smarttodo.app.client;

import com.smarttodo.app.bot.InlineKeyboardBuilder;
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
        var body = InlineKeyboardBuilder.create()
                .text("""
                        Привет! Я твой помощник по самоорганизации. Помогу планировать день, вести задачи, отслеживать часы активности и формировать полезные привычки.

                        Что я делаю:
                        • Быстро добавляю и напоминаю о задачах
                        • Следую за прогрессом и показываю статистику выполнения
                        • Замеряю «часы активности» — когда ты реально делаешь дела
                        • Запускаю трекеры привычек и мотивирую не срываться
                        """
                )
                .format("markdown")
                .addCallbackButton("✅Задачи", "tasks-handler")
                .addCallbackButton("🗓️Привычки", "habit-handler")
                .addCallbackButton("⏰Напоминания", "notification-handler")
                .build();

        postMessage(chatId, body)             // твой внутренний метод
                .timeout(TIMEOUT)
                .retryWhen(RETRY_5XX_OR_NETWORK)
                .block();
    }

    public void sendTaskKeyboard(long chatId) {
        var body = InlineKeyboardBuilder.create()
                .text("""
              📝**Меню задач**
              """)
                .format("markdown")
                .addCallbackButton("Задачи на сегодня", "tasks-get-today")
                .addCallbackButton("Задачи на неделю", "tasks-get-week")
                .addCallbackButton("Создать задачу", "tasks-create-new")
                .build();

        postMessage(chatId, body)
                .timeout(TIMEOUT)
                .retryWhen(RETRY_5XX_OR_NETWORK)
                .block();
    }

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
