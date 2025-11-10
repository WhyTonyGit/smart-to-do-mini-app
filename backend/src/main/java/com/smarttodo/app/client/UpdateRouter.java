package com.smarttodo.app.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttodo.app.dto.Update;
import com.smarttodo.app.entity.TaskStatus;
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
    private final UserService userService;
    private final TaskService taskService;
    private final HabitService habitService;

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
            return;
        }

        if (u.isCallback("tasks-handler")) {
            max.sendText(u.chatId(), "Пока в разработке.");
            return;
        }
        if (u.isCallback("habit-handler")) {
            max.sendText(u.chatId(), "Пока в разработке.");
            return;
        }
        if (u.isCallback("notification-handler")) {
            max.sendText(u.chatId(), "Пока в разработке.");
            return;
        }
        if (u.isCallback("tasks-create-new")) {
            max.sendText(u.chatId(), "Пока в разработке.");
            return;
        }
    }

    private String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 500 ? s.substring(0, 500) + "…[cut]" : s;
    }
}
