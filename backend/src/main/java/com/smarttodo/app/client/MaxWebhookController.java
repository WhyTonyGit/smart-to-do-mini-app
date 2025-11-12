package com.smarttodo.app.client;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class MaxWebhookController {

    private final UpdateRouter router;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> onUpdate(@RequestBody String rawJson,
                                         @RequestHeader Map<String, String> headers) {
        log.info("INBOUND webhook: headers={}", headers);
        log.info("INBOUND webhook body: {}", truncate(rawJson));
        router.dispatch(rawJson);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
