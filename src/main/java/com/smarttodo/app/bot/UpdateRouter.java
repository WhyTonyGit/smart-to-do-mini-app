package com.smarttodo.app.bot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttodo.app.client.MaxApi;
import com.smarttodo.app.dto.MessageMeta;
import com.smarttodo.app.entity.Update;
import com.smarttodo.app.llm.NlpService;
import com.smarttodo.app.repository.LastActionRedisRepo;   // <-- синхронный репозиторий
import com.smarttodo.app.service.HabitService;
import com.smarttodo.app.service.TaskService;
import com.smarttodo.app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UpdateRouter {
    private static final Logger log = LoggerFactory.getLogger(UpdateRouter.class);

    private final ObjectMapper om;
    private final MessageSender messageSender;
    private final NlpService nlp;
    private final MaxApi maxApi;

    private final UserService userService;
    private final TaskService taskService;
    private final HabitService habitService;
    private final LastActionRedisRepo lastActionRepo;// <-- заменили тип
    private final TaskManager taskManager;

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
        // верхнеуровневые логи для отладки
        try {
            log.info("ROUTE payload: {}", om.writeValueAsString(u));
        } catch (Exception e) {
            log.debug("ROUTE payload json serialization failed: {}", e.toString());
        }
        log.info("Update: {}, UserId: {}", u, u.userId());
        log.info("Is text command /start: {}", u.isTextCommand("/start"));

        // Команда /start
        if (u.isTextCommand("/start")) {
            log.info("ROUTE: /start for chatId={}", u.chatId());
            if (!userService.isUserExists(u.userId())) {
                userService.createUser(u.userId(), u.chatId(), null);
            }
            messageSender.sendStartKeyboard(u.chatId());
            return;
        }

        // Обычный текст
        if (u.isText()) {
            log.info("ROUTE: handle text, chatId={}", u.chatId());

            Optional<MessageMeta> opt = lastActionRepo.get(u.chatId());
            MessageMarker marker = opt.map(MessageMeta::marker).orElse(null);
            String mid = opt.map(MessageMeta::mid).orElse(null);
            log.debug("Redis marker for chatId={} -> {}", u.chatId(), marker);

            if (marker == null) {
                log.info("No marker -> fallback hint, chatId={}", u.chatId());
                messageSender.sendText(u.chatId(), "Пу-пу-пуу, попробуйте сначала");
                return;
            }

            switch (marker) {
                case CREATE_TASK -> {
                    log.info("Marker=TASK_MENU -> creating task flow, chatId={}", u.chatId());

                    taskManager.parseTextWithLlm(u);
                }
                case CHANGE_TASK_TITLE -> {
                    log.info("Marker=CHANGE_TASK_TITLE -> creating task flow, chatId={}", u.chatId());

                    taskManager.changeTaskTitle(u);
                }
                case CHANGE_TASK_DESCRIPTION -> {
                    log.info("Marker=CHANGE_TASK_DESCRIPTION -> creating task flow, chatId={}", u.chatId());

                    taskManager.changeTaskDescription(u);
                }
                case CHANGE_TASK_DEADLINE -> {
                    log.info("Marker=CHANGE_TASK_DEADLINE -> creating task flow, chatId={}", u.chatId());

                    taskManager.changeTaskDeadline(u);
                }
                default -> {
                    log.info("Unknown marker={} -> fallback, chatId={}", marker, u.chatId());
                    messageSender.sendText(u.chatId(), "Нераспознанный контекст");
                }
            }
            return;
        }

        // Callback-кнопки
        if (u.isCallback()) {
            log.info("ROUTE: handle callback, chatId={}, userId={}, payload={}", u.chatId(), u.userId(), u.getPayload());

            if (Objects.equals(u.getPayload().split(":")[0], "task-id")) {
                taskManager.pickTask(u);
            }

            switch (u.getPayload()) {
                case "tasks-handler" -> messageSender.sendTaskKeyboard(u.chatId());
                case "habit-handler" -> messageSender.sendText(u.chatId(), "Пока в разработке.");
                case "notification-handler" -> messageSender.sendText(u.chatId(), "Пока в разработке..");
                case "tasks-create-new" -> taskManager.createTask(u);
                case "tasks-change-title" -> messageSender.sendInputTaskTitle(u.chatId());
                case "tasks-change-description" -> messageSender.sendInputTaskDescription(u.chatId());
                case "tasks-change-deadline" -> messageSender.sendInputTaskDeadline(u.chatId());
                case "tasks-create-confirm" -> taskManager.confirmTaskCreating(u);
                case "tasks-get-today" -> taskManager.getTodayTaskList(u);
                case "tasks-get-week" -> taskManager.getWeekTaskList(u);
                case "tasks-get-all" -> taskManager.getAllTaskList(u);
                case "home-page" -> messageSender.sendHomePageKeyboard(u.chatId());
                default -> messageSender.sendText(u.chatId(), "Произошла ошибка на сервере, приносим свои извинения.");
            }
            return;
        }

        // тут llm (оставляю как в твоём варианте; при необходимости тоже переведем на императивный вызов)
//        if (u.isType("message_created") && u.getMessage() != null) {
//            var body = u.getMessage().getBody();
//            var text = body != null ? body.getText() : null;
//            if (text != null && !text.isBlank()) {
//                try {
//                    var parsed = nlp.parseText(text).block(java.time.Duration.ofSeconds(12));
//                    if (parsed == null || parsed.tasks() == null || parsed.tasks().isEmpty()) {
//                        messageSender.sendText(u.chatId(), "Не смог разобрать задачу. Сформулируй чуть яснее?");
//                        return;
//                    }
//                    var sb = new StringBuilder("Разобрал так:\n");
//                    int i = 1;
//                    for (var t : parsed.tasks()) {
//                        sb.append(i++).append(". ")
//                                .append(t.title() != null ? t.title() : "—");
//                        if (t.description() != null && !t.description().isBlank()) {
//                            sb.append(" (").append(t.description()).append(")");
//                        }
//                        if (t.date() != null || t.time() != null) {
//                            sb.append(" — ");
//                            if (t.date() != null) sb.append(t.date());
//                            if (t.date() != null && t.time() != null) sb.append(" ");
//                            if (t.time() != null) sb.append(t.time());
//                        }
//                        if (t.splitOf() != null) sb.append(" [группа ").append(t.splitOf()).append("]");
//                        sb.append("\n");
//                    }
//                    messageSender.sendText(u.chatId(), sb.toString());
//                    return;
//                } catch (Exception e) {
//                    messageSender.sendText(u.chatId(), "Упс, модель не ответила вовремя. Попробуем ещё раз позже.");
//                    return;
//                }
//            }
//        }

        log.info("Unhandled update_type={} eventId={}", u.getUpdateType(), u.getEventId());
    }

    private String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 500 ? s.substring(0, 500) + "…[cut]" : s;
    }
}
