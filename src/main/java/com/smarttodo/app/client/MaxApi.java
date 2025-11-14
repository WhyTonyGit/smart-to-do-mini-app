package com.smarttodo.app.client;

import com.smarttodo.app.bot.InlineKeyboardBuilder;
import com.smarttodo.app.dto.MessageMeta;
import com.smarttodo.app.dto.SendMessageResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.message.Message;
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



    private final WebClient client; // собран в MaxConfig: baseUrl + Authorization

    public MaxApi(@Qualifier("maxClient") WebClient client) {
        this.client = client;
    }

    public Mono<SendMessageResult> postMessage(long chatId, Object body) {
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
                .bodyToMono(SendMessageResult.class);
    }

    public Mono<ResponseEntity<Void>> editMessage(String messageId, Object body) {
        final long started = System.nanoTime();
        log.info("Start editMessage");
        return client.put()
                .uri(b -> b.path("/messages")
                        .queryParam("message_id", messageId)
                        .build())
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
                .toBodilessEntity()
                .doOnSubscribe(s ->
                        log.info("PUT /messages start: messageId={}, body={}", messageId, body))
                .doOnSuccess(resp ->
                        log.info("PUT /messages ok: messageId={}, status={}", messageId, resp.getStatusCode()))
                .doOnError(e ->
                        log.warn("PUT /messages failed: messageId={}, err={}", messageId, e.toString()))
                .doFinally(sig ->
                        log.debug("PUT /messages finished: messageId={}, signal={}, took={} ms",
                                messageId, sig, (System.nanoTime() - started) / 1_000_000));
    }


    public static boolean isTransient(Throwable t) {
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
