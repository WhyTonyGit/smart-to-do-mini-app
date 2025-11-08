package com.smarttodo.app.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttodo.app.dto.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UpdateRouter {
    private static final Logger log = LoggerFactory.getLogger(UpdateRouter.class);

    private final ObjectMapper om;
    private final MaxApi max;

    public UpdateRouter(ObjectMapper om, MaxApi max) {
        this.om = om;
        this.max = max;
    }

    /** Главная точка входа: вызывается контроллером вебхука */
    public void dispatch(String rawJson) {
        final Update u = parse(rawJson);

        // Роутинг по типу события и содержимому
        try {
            route(u);
        } catch (Exception e) {
            // падать нельзя — MAX ждёт 200 OK; логируем и двигаемся дальше
            log.error("Handler error for updateType={} eventId={}: {}",
                    u.getUpdateType(), u.getEventId(), e.toString(), e);
        }
    }

    private Update parse(String raw) {
        try {
            return om.readValue(raw, Update.class);
        } catch (Exception e) {
            // плохой вход — лог и выходим; можно метрику «bad_payload»
            log.warn("Bad payload: {}", truncate(raw), e);
            // возвращаем пустой Update, чтобы безопасно упасть в route()
            return new Update();
        }
    }

    private void route(Update u) {
        // Пример 1: команды
        if (u.isTextCommand("/start")) {
            max.sendStartKeyboard(u.chatId());
            max.sendOpenLink(u.chatId(), "https://dev.max.ru/docs/webapps/bridge", "press");
            return;
        }

        if (u.isCallback("start")) {
            max.sendText(u.chatId(), "Пока в разработке.");
            return;
        }

        // Пример 2: эхо любого текста
        if (u.isType("message_created") && u.getMessage() != null) {
            var body = u.getMessage().getBody();
            var text = body != null ? body.getText() : null;
            if (text != null && !text.isBlank()) {
                max.sendText(u.chatId(), "Вы сказали: " + text);
                return;
            }
        }

        log.info("Unhandled update_type={} eventId={}", u.getUpdateType(), u.getEventId());
    }

    private String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 500 ? s.substring(0, 500) + "…[cut]" : s;
    }
}
