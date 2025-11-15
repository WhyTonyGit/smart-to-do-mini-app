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
    private final HabitManager habitManager;

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
            if (!userService.iSUserExists(u.userId())) {
                userService.createUser(u.userId(), u.chatId(), null);
            }
            messageSender.sendStartKeyboard(u.chatId());
            return;
        }

        if (u.isText()) {
            log.info("ROUTE: handle text, chatId={}", u.chatId());

            Optional<MessageMeta> opt = lastActionRepo.get(u.chatId());
            MessageMarker marker = opt.map(MessageMeta::marker).orElse(null);
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
                case CHANGE_HABIT_TITLE -> {
                    log.info("Marker=CHANGE_HABIT_TITLE -> creating habit flow, chatId={}", u.chatId());
                    habitManager.changeHabitTitle(u);
                }
                case CHANGE_HABIT_DESCRIPTION -> {
                    log.info("Marker=CHANGE_HABIT_DESCRIPTION -> creating habit flow, chatId={}", u.chatId());
                    habitManager.changeHabitDescription(u);
                }
                case CHANGE_HABIT_INTERVAL -> {
                    log.info("Marker=CHANGE_HABIT_INTERVAL -> creating habit flow, chatId={}", u.chatId());
                    habitManager.changeHabitInterval(u);
                }
                case CHANGE_HABIT_GOAL_DATE -> {
                    log.info("Marker=CHANGE_HABIT_GOAL_DATE -> creating habit flow, chatId={}", u.chatId());
                    habitManager.changeHabitGoalDate(u);
                }
                default -> {
                    log.info("Unknown marker={} -> fallback, chatId={}", marker, u.chatId());
                    messageSender.sendText(u.chatId(), "Нераспознанный контекст");
                }
            }
            return;
        }

        if (u.isCallback()) {
            log.info("ROUTE: handle callback, chatId={}, userId={}, payload={}", u.chatId(), u.userId(), u.getPayload());

            Payload payload = Payload.from(u.getPayload());
            if (payload == null) {
                log.error("Unknown task payload: {}", u.getPayload());
                return;
            }

            log.info("Checking payload {} hasId: {}", u.getPayload(), payload.hasId());
            if (payload.hasId()) {
                if (payload.isTasksPayload()) {
                    taskManager.pickTask(u);
                    return;
                }
                if (payload.isHabitsPayload()) {
                    habitManager.pickHabit(u);
                    return;
                }
            }

            switch (payload) {
                case TASK_MENU -> messageSender.sendTaskKeyboard(u.chatId());
                case TASKS_CREATE_NEW -> taskManager.createTask(u);
                case TASKS_CHANGE_TITLE -> messageSender.sendInputTaskTitle(u.chatId());
                case TASKS_CHANGE_DESCRIPTION -> messageSender.sendInputTaskDescription(u.chatId());
                case TASKS_CHANGE_DEADLINE -> messageSender.sendInputTaskDeadline(u.chatId());
                case TASKS_CREATE_CONFIRM -> taskManager.confirmTaskCreating(u);
                case TASKS_GET_TODAY -> taskManager.getTodayTaskList(u);
                case TASKS_GET_WEEK -> taskManager.getWeekTaskList(u);
                case TASKS_GET_ALL -> taskManager.getAllTaskList(u);
                case TASKS_GET_TOMORROW -> taskManager.getTomorrowTaskList(u);

                case HABIT_MENU -> messageSender.sendHabitKeyboard(u.chatId());
                case HABITS_CREATE_NEW -> habitManager.createHabit(u);
                case HABITS_CHANGE_TITLE ->  messageSender.sendHabitTitleInput(u.chatId());
                case HABITS_CHANGE_DESCRIPTION ->   messageSender.sendHabitDescriptionInput(u.chatId());
                case HABITS_CHANGE_INTERVAL ->   messageSender.sendHabitIntervalInput(u.chatId());
                case HABITS_CHANGE_GOAL_DATE ->   messageSender.sendHabitGoalDateInput(u.chatId());
                case HABITS_GET_ALL -> habitManager.getAllHabitsList(u);
                case HABITS_GET_TODAY -> habitManager.getTodayHabitsList(u);
                case HABITS_GET_WEEK -> habitManager.getWeekHabitsList(u);
                case HABITS_CREATE_CONFIRM ->  habitManager.confirmHabitCreating(u);

                case HOME_PAGE -> messageSender.sendHomePageKeyboard(u.chatId());

                default -> messageSender.sendText(u.chatId(), "Ошибка");
            }
            return;
        }

        log.info("Unhandled update_type={} eventId={}", u.getUpdateType(), u.getEventId());
    }

    private String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 500 ? s.substring(0, 500) + "…[cut]" : s;
    }
}
