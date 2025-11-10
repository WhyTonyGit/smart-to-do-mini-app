package com.smarttodo.app.client;

import com.smarttodo.app.bot.UpdateRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.util.StringUtils.truncate;

@Slf4j
@RestController
@RequestMapping("/webhook/max")
public class MaxWebhookController {

    private final UpdateRouter router;

    public MaxWebhookController(UpdateRouter router) {
        this.router = router;
    }

    /**
     * Главная точка приёма событий MAX.
     * Требуем application/json, иначе 415.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> onUpdate(@RequestBody String rawJson,
                                         @RequestHeader Map<String, String> headers) {
        log.info("INBOUND webhook: headers={}", headers);           // если нужно
        log.info("INBOUND webhook body: {}", truncate(rawJson));

        //Передаём в маршрутизатор (он сам парсит JSON, делает идемпотентность и логику)
        router.dispatch(rawJson);

        //Всегда 200 OK — MAX получит «доставлено» (даже если логика внутри дала ошибку, мы её залогировали)
        return ResponseEntity.ok().build();
    }

    /**
     * Простая проверка, что эндпоинт жив.
     * Удобно давать MAX для initial verification (если требуется).
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
