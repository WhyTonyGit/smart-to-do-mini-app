package com.smarttodo.app.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttodo.app.dto.Update;
import com.smarttodo.app.llm.NlpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateRouter {

    private final ObjectMapper om;
    private final MaxApi max;
    private final NlpService nlp;

    public void dispatch(String rawJson) {
        final Update u = parse(rawJson);
        try {
            route(u);
        } catch (Exception e) {
            log.error("Handler error for updateType={} eventId={}: {}", u.getUpdateType(), u.getEventId(), e.toString(), e);
        }
    }

    private Update parse(String raw) {
        try {
            return om.readValue(raw, Update.class);
        } catch (Exception e) {
            log.warn("Bad payload: {}", truncate(raw), e);
            return new Update();
        }
    }

    private void route(Update u) {
        if (u.isTextCommand("/start")) {
            max.sendStartKeyboard(u.chatId());
            max.sendOpenLink(u.chatId(), "https://dev.max.ru/docs/webapps/bridge", "press");
            return;
        }

        if (u.isCallback("start")) {
            max.sendText(u.chatId(), "Пока в разработке.");
            return;
        }

        if (u.isType("message_created") && u.getMessage() != null) {
            var body = u.getMessage().getBody();
            var text = body != null ? body.getText() : null;
            if (text != null && !text.isBlank()) {
                try {
                    var parsed = nlp.parseText(text).block(java.time.Duration.ofSeconds(12));
                    if (parsed == null || parsed.tasks() == null || parsed.tasks().isEmpty()) {
                        max.sendText(u.chatId(), "Не смог разобрать задачу. Сформулируй чуть яснее?");
                        return;
                    }
                    var sb = new StringBuilder("Разобрал так:\n");
                    int i = 1;
                    for (var t : parsed.tasks()) {
                        sb.append(i++).append(". ")
                                .append(t.title() != null ? t.title() : "—");
                        if (t.description() != null && !t.description().isBlank()) {
                            sb.append(" (").append(t.description()).append(")");
                        }
                        if (t.date() != null || t.time() != null) {
                            sb.append(" — ");
                            if (t.date() != null) sb.append(t.date());
                            if (t.date() != null && t.time() != null) sb.append(" ");
                            if (t.time() != null) sb.append(t.time());
                        }
                        if (t.splitOf() != null) sb.append(" [группа ").append(t.splitOf()).append("]");
                        sb.append("\n");
                    }
                    max.sendText(u.chatId(), sb.toString());
                    return;
                } catch (Exception e) {
                    max.sendText(u.chatId(), "Упс, модель не ответила вовремя. Попробуем ещё раз позже.");
                    return;
                }
            }
        }

        log.info("Unhandled update_type={} eventId={}", u.getUpdateType(), u.getEventId());
    }

    private String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 500 ? s.substring(0, 500) + "…[cut]" : s;
    }
}
