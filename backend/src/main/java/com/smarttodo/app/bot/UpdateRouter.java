package com.smarttodo.app.bot;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttodo.app.client.MaxApi;
import com.smarttodo.app.dto.Update;
import com.smarttodo.app.llm.NlpService;
import com.smarttodo.app.service.HabitService;
import com.smarttodo.app.service.TaskService;
import com.smarttodo.app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateRouter {
    private static final Logger log = LoggerFactory.getLogger(UpdateRouter.class);

    private final ObjectMapper om;
    private final MaxApi max;
    private final NlpService nlp;

    private final UserService userService;
    private final TaskService taskService;
    private final HabitService habitService;

    /** Главная точка входа: вызывается контроллером вебхука */
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

    private void route(Update u) throws JsonProcessingException {
        log.info("ROUTE: {}", om.writeValueAsString(u));
        log.info("Update: {}", u.toString());
        log.info("Is text command: {}", u.isTextCommand("/start"));

        // Пример 1: команды
        if (u.isTextCommand("/start")) {
            log.info("ROUTE: start");
            max.sendStartKeyboard(u.chatId());
            return;
        }

        // закомментил твой кусок, потому что ниже кусок кода конфликтует с этим, а там llm встроена. Нужно их соединить, чтобы всё работало
//        if (u.isCallback()) {
//            switch (u.getPayload()) {
//                case "tasks-handler" -> {
//                    max.sendTaskKeyboard(u.chatId());
//                    break;
//                }
//                case "habit-handler" -> {
//                    max.sendText(u.chatId(), "Пока в разработке.");
//                    break;
//                }
//                case "notification-handler" -> {
//                    max.sendText(u.chatId(), "Пока в разработке..");
//                    break;
//                }
//                case "tasks-create-new" -> {
//                    max.sendText(u.chatId(), "Пока в разработке...");
//                    break;
//                }
//                default -> {
//                    max.sendText(u.chatId(), "Произошла ошибка на сервере, приносим свои извинения.");
//                    break;
//                }
//            }
//        }

        // тут llm
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
