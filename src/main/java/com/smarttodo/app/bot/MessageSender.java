package com.smarttodo.app.bot;

import com.smarttodo.app.client.MaxApi;
import com.smarttodo.app.dto.*;
import com.smarttodo.app.entity.HabitInterval;
import com.smarttodo.app.entity.TaskStatus;
import com.smarttodo.app.repository.LastActionRedisRepo;
import com.smarttodo.app.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSender {

    private final MaxApi maxApi;        // postMessage возвращает Mono<SendMessageResult>
    private final LastActionRedisRepo lastRepo;   // синхронный репозиторий
    private final MetricsService metricsService;

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Retry RETRY_5XX_OR_NETWORK = Retry
            .backoff(3, Duration.ofMillis(300))
            .filter(MaxApi::isTransient);

    public void sendText(long chatId, String text) {
        if (chatId <= 0) throw new IllegalArgumentException("chatId must be > 0");
        if (text == null || text.isBlank()) return;

        Map<String, Object> body = Map.of("text", text);

        try {
            log.info("POST /messages start: chatId={}, kind=TEXT", chatId);
            // блокирующий вызов с timeout и retry
            var res = maxApi.postMessage(chatId, body)
                    .timeout(TIMEOUT)
                    .retryWhen(RETRY_5XX_OR_NETWORK)
                    .block();

            if (res == null) {
                throw new IllegalStateException("Empty SendMessageResult");
            }

            log.info("TEXT sent: chatId={}, mid={}, seq={}", chatId, res.getMid(), res.getSeq());
        } catch (Exception e) {
            log.warn("sendText failed: chatId={}, err={}", chatId, e.toString());
        }
    }

    public void sendStartKeyboard(long chatId) {
        var body = InlineKeyboardBuilder.create()
                .text("""
                        
                        Привет! Я твой помощник по самоорганизации. Готов держать фокус, планировать день и превращать хаос в аккуратные победы 💪:
                        
                        • ⚡️ Быстро добавляю задачи и напоминаю о важных делах
                        • 📊 Показываю реальный прогресс и красивую статистику
                        • ⏱️ Замеряю твои «часы активности» — когда ты действительно работаешь
                        • 🌱 Запускаю трекеры привычек и помогаю не выбиваться из ритма
                        
                        Готов начать? 🚀
                        """)
                .format("markdown")
                .addCallbackButton("📋 Меню задач",   "tasks-menu")
                .addCallbackButton("🗓️ Меню привычек", "habits-menu")
                .build();

        sendMessage(chatId, body, MessageMarker.WELCOME);
    }

    public void sendInputTaskTitle(long chatId) {
        String text = """
                Введите название задачи:
                """;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);

        sendMessage(chatId, body, MessageMarker.CHANGE_TASK_TITLE);
    }

    public void sendInputTaskDescription(long chatId) {
        String text = """
                Введите описание задачи:
                """;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);

        sendMessage(chatId, body, MessageMarker.CHANGE_TASK_DESCRIPTION);
    }

    public void sendInputTaskDeadline(long chatId) {
        String text = """
                Введите дэдлайн по задаче в формате dd.MM.yyyy HH:mm:
                """;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);

        sendMessage(chatId, body, MessageMarker.CHANGE_TASK_DEADLINE);
    }

    public Object createTaskCreateKeyboardBody(String title, String description, String deadline) {

        return InlineKeyboardBuilder.create()
                .text("""
                        ✨ Предпросмотр задачи

                        Название: %s
                        Описание: %s
                        Дэдлайн: %s

                        Вы можете описать задачу текстом, поля выше заполнятся автоматически или заполнить содержимое с помощью кнопок.
                        Обязательно укажите дату и время дэдлайна по задаче.
                        Обработка текста занимает 10 - 30 секунд.
                        Затем вы сможете с помощью кнопок отредактировать задачу.
                        """.formatted(
                        title == null ? "." : title,
                        description == null ? "." : description,
                        deadline == null ? "." : deadline
                ))
                .format("markdown")
                .addCallbackButton("Изменить название",     Payload.TASKS_CHANGE_TITLE.key())
                .addCallbackButton("Изменить описание",     Payload.TASKS_CHANGE_DESCRIPTION.key())
                .addCallbackButton("Изменить дэдлайн",      Payload.TASKS_CHANGE_DEADLINE.key())
                .addCallbackButton("Подтвердить создание",  Payload.TASKS_CREATE_CONFIRM.key())
                .addCallbackButton("Меню привычек",        Payload.HABIT_MENU.key())
                .addCallbackButton("Меню задач",        Payload.TASK_MENU.key())
                .addCallbackButton("Вернуться в главное меню",        Payload.HOME_PAGE.key())
                .build();
    }

    public void sendTaskCreateKeyboard(long chatId) {

        var body = InlineKeyboardBuilder.create()
                .text("""
                        ✨ Предпросмотр задачи

                        Название: .
                        Описание: .
                        Дэдлайн: .
                        
                        Вы можете описать задачу текстом, поля выше заполнятся автоматически или заполнить содержимое с помощью кнопок.
                        Обязательно укажите дату и время дэдлайна по задаче.
                        Обработка текста занимает 10 - 30 секунд.
                        Затем вы сможете с помощью кнопок отредактировать задачу.
                        """)
                .format("markdown")
                .addCallbackButton("Изменить название",     Payload.TASKS_CHANGE_TITLE.key())
                .addCallbackButton("Изменить описание",     Payload.TASKS_CHANGE_DESCRIPTION.key())
                .addCallbackButton("Изменить дэдлайн",      Payload.TASKS_CHANGE_DEADLINE.key())
                .addCallbackButton("Подтвердить создание",  Payload.TASKS_CREATE_CONFIRM.key())
                .addCallbackButton("Меню привычек",        Payload.HABIT_MENU.key())
                .addCallbackButton("Меню задач",        Payload.TASK_MENU.key())
                .addCallbackButton("Вернуться в главное меню",        Payload.HOME_PAGE.key())
                .build();

        sendMessage(chatId, body, MessageMarker.CREATE_TASK);
    }

    public void sendHomePageKeyboard(long chatId) {
        String text;

        try {
            var summary    = metricsService.getWeeklySummary(chatId);
            var habitStats = summary.habitStats();
            var taskStats  = summary.taskStats();

            var dateFormatter = DateTimeFormatter.ofPattern("dd.MM");
            String period = summary.weekStart().format(dateFormatter)
                    + "–"
                    + summary.weekEnd().format(dateFormatter);

            long totalTasks = taskStats != null ? taskStats.totalTasks()      : 0;
            long completedTasks = taskStats != null ? taskStats.completedTasks()  : 0;
            long overdueTasks = taskStats != null ? taskStats.overdueTasks()    : 0;

            int totalHabits      = habitStats != null ? habitStats.totalHabits()   : 0;
            int activeHabits     = habitStats != null ? habitStats.activeHabits()  : 0;

            double avgHabitCompletion = 0.0;
            if (habitStats != null
                    && habitStats.completionRates() != null
                    && !habitStats.completionRates().isEmpty()) {
                avgHabitCompletion = habitStats.completionRates().values().stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
            }

            int activeDaysCount = summary.activeDays() != null
                    ? summary.activeDays().size()
                    : 0;

            text = """
                   👤 **Твой профиль**

                   _Неделя: %s_

                   **Задачи**📅
                   • Всего: %d
                   • Выполнено: %d
                   • Просрочено: %d

                   **Привычки**🌱
                   • Всего: %d
                   • Активных: %d
                   • Средний прогресс: %.0f%%

                   **Активность**📊
                   • Дней с выполненными задачами: %d из 7
                   """.formatted(
                    period,
                    totalTasks,
                    completedTasks,
                    overdueTasks,
                    totalHabits,
                    activeHabits,
                    avgHabitCompletion,
                    activeDaysCount
            );
        } catch (Exception e) {
            log.warn("Failed to build profile metrics for chatId={}: {}", chatId, e.toString());
            text = """
                   👤 **Твой профиль**

                   Пока нет данных по задачам и привычкам за эту неделю.
                   Начни с того, чтобы добавить задачу или создать привычку 🙂
                   """;
        }

        var body = InlineKeyboardBuilder.create()
                .text(text)
                .format("markdown")
                // две кнопки профиля
                .addCallbackButton("📋 Меню задач",   "tasks-menu")
                .addCallbackButton("🗓️ Меню привычек", "habits-menu")
                .build();

        sendMessage(chatId, body, MessageMarker.HOME_MENU);
    }

    public void sendTodayTaskList(long chatId, List<TaskDto> tasks) {
        sendTaskList(chatId, tasks, "Список задач на сегодня");
    }

    public void sendWeekTaskList(long chatId, List<TaskDto> tasks) {
        sendTaskList(chatId, tasks, "Список задач на неделю");
    }

    public void sendAllTaskList(long chatId, List<TaskDto> tasks) {
        sendTaskList(chatId, tasks, "Список всех незавершенных задач");
    }

    public void sendTask(long chatId, TaskDto task) {
        var body = InlineKeyboardBuilder.create()
                .text("""
                        %s **%s**
                        Описание: %s
                        Дэдлайн: %s
                        Статус: %s
                        """.formatted(
                        task.status().getEmoji(),
                        task.title(),
                        task.description(),
                        TaskManager.formatLocalDateTime(task.deadline()),
                        task.status().getDescription()
                ))
                .format("markdown")
                .addCallbackButton("Отметить невыполненной",   Payload.TASKS_SET_STATUS_UNCOMPLETED.key() + ":" + task.id())
                .addCallbackButton("Отметить взятой в работу",  Payload.TASKS_SET_STATUS_IN_PROGRESS.key() + ":" + task.id())
                .addCallbackButton("Отметить выполненной",   Payload.TASKS_SET_STATUS_COMPLETED.key() + ":" + task.id())
                .addCallbackButton("Удалить задачу",         Payload.TASKS_DELETE.key() + ":" + task.id())
                .addCallbackButton("Меню привычек",        Payload.HABIT_MENU.key())
                .addCallbackButton("Меню задач",        Payload.TASK_MENU.key())
                .addCallbackButton("Вернуться в главное меню",        Payload.HOME_PAGE.key())
                .build();

        sendMessage(chatId, body, MessageMarker.TASK_LIST);
    }

    public void sendHabit(long chatId, HabitCheckinDto habit) {
        String description = (habit.description() == null || habit.description().isBlank())
                ? "_Описание не задано_"
                : habit.description();

        String interval = habit.interval() != null
                ? habit.interval().getDisplayName()
                : "не задана";

        String goalDate = habit.goalDate() != null
                ? habit.goalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                : "не задана";

        boolean shouldDo = HabitManager.shouldDoToday(habit);
        InlineKeyboardBuilder body;
        if (shouldDo) {
            body = InlineKeyboardBuilder.create()
                    .text("""
                   %s **%s**
                   Необходимо выполнить сегодня: %s
                   Выполнена: %s
                   Описание: %s
                   Периодичность: %s
                   Цель до: %s
                   Статус: %s
                   """.formatted(
                            habit.status().getEmoji(),
                            habit.title(),
                            "да",
                            habit.isCompletedOnTime() ? "✅" : "❌",
                            description,
                            interval,
                            goalDate,
                            habit.status().getDescription()
                    ))
                    .format("markdown")
                    .addCallbackButton("Отметить невыполненной сегодня", Payload.HABITS_MARK_AS_UNCOMPLETED.key() + ":%s".formatted(habit.id()))
                    .addCallbackButton("Отметить выполненной сегодня", Payload.HABITS_MARK_AS_COMPLETED.key() + ":%s".formatted(habit.id()));

        } else {
            body = InlineKeyboardBuilder.create()
                    .text("""
                   %s **%s**
                   Необходимо выполнить сегодня: %s
                   Описание: %s
                   Периодичность: %s
                   Цель до: %s
                   Статус: %s
                   """.formatted(
                            habit.status().getEmoji(),
                            habit.title(),
                            "нет",
                            description,
                            interval,
                            goalDate,
                            habit.status().getDescription()
                    ))
                    .format("markdown");
        }

        body.addCallbackButton("Поставить статус: завершена", Payload.HABITS_SET_STATUS_ARCHIVED.key() + ":%s".formatted(habit.id()))
                .addCallbackButton("Поставить статус: в процессе", Payload.HABITS_SET_STATUS_IN_PROGRESS.key() + ":%s".formatted(habit.id()))
                .addCallbackButton("Поставить статус: приостановлена", Payload.HABITS_SET_STATUS_PAUSED.key() + ":%s".formatted(habit.id()))
                .addCallbackButton("Меню привычек",        Payload.HABIT_MENU.key())
                .addCallbackButton("Меню задач",        Payload.TASK_MENU.key())
                .addCallbackButton("Вернуться в главное меню",        Payload.HOME_PAGE.key());

        sendMessage(chatId, body.build(), MessageMarker.HABIT_LIST);
    }

    private void sendTaskList(long chatId, List<TaskDto> tasks, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(title).append("**\n\n");

        for (var task : tasks) {
            if (task.status() == TaskStatus.COMPLETED) {
                sb.append("""
                        ~~%s **%s**~~
                        """.formatted(
                        task.status().getEmoji(),
                        task.title()
                ));
            } else {
                sb.append("""
                        %s **%s**
                        Описание: %s
                        Дэдлайн: %s
                        """.formatted(
                        task.status().getEmoji(),
                        task.title(),
                        task.description(),
                        TaskManager.formatLocalDateTime(task.deadline())
                ));
            }
        }

        sb.append("\n*Кликните на задачу, чтобы перейти к ней*");

        var body = InlineKeyboardBuilder.create()
                .text(sb.toString())
                .format("markdown");

        for (var task : tasks) {
            body.addCallbackButton(
                    task.status().getEmoji() + ' ' + task.title(),
                    Payload.TASKS_ID.key() + ":%s".formatted(task.id())
            );
        }

        body.addCallbackButton("➕ Создать задачу", Payload.TASKS_CREATE_NEW.key());
        body.addCallbackButton("Главное меню",  Payload.HOME_PAGE.key());

        sendMessage(chatId, body.build(), MessageMarker.TASK_LIST);
    }

    public void sendTaskKeyboard(long chatId) {
        var body = InlineKeyboardBuilder.create()
                .text("""
                        📝 **Меню задач**

                        Здесь ты можешь посмотреть задачи на сегодня, неделю,
                        увидеть все незавершенные задачи и добавить новые.
                        """)
                .format("markdown")
                .addCallbackButton("✅ Все незавершенные", Payload.TASKS_GET_ALL.key())
                .addCallbackButton("📅 На сегодня",         Payload.TASKS_GET_TODAY.key())
                .addCallbackButton("📆 На неделю",          Payload.TASKS_GET_WEEK.key())
                .addCallbackButton("📆 На завтра",          Payload.TASKS_GET_TOMORROW.key())
                .addCallbackButton("➕ Создать задачу",      Payload.TASKS_CREATE_NEW.key())
                .addCallbackButton("🏠 В профиль",          Payload.HOME_PAGE.key())
                .build();

        sendMessage(chatId, body, MessageMarker.TASK_MENU);
    }

    public void sendHabitKeyboard(long chatId) {
        var body = InlineKeyboardBuilder.create()
                .text("""
                        🧩 **Меню привычек**

                        Здесь ты можешь отслеживать свои привычки, смотреть,
                        что нужно сделать сегодня и на неделе, а также следить
                        за своими сериями.
                        """)
                .format("markdown")
                .addCallbackButton("💪 Все привычки",        Payload.HABITS_GET_ALL.key())
                .addCallbackButton("📅 На сегодня",          Payload.HABITS_GET_TODAY.key())
//                .addCallbackButton("📅 На неделю",           Payload.HABITS_GET_WEEK.key())
//                .addCallbackButton("🔥 Текущие серии",       Payload.HABITS_STREAKS.key())
                .addCallbackButton("➕ Создать привычку",    Payload.HABITS_CREATE_NEW.key())
                .addCallbackButton("🏠 В профиль",           Payload.HOME_PAGE.key())
                .build();

        sendMessage(chatId, body, MessageMarker.HABIT_MENU);
    }

    private void sendMessage(long chatId, Object body, MessageMarker marker) {
        try {
            log.info("POST /messages start: chatId={}, marker={}", chatId, marker);

            var res = maxApi.postMessage(chatId, body)
                    .timeout(TIMEOUT)
                    .retryWhen(RETRY_5XX_OR_NETWORK)
                    .block();

            if (res == null) throw new IllegalStateException("Empty SendMessageResult");

            log.debug("POST /messages ok: mid={}, seq={}, ts={}", res.getMid(), res.getSeq(), res.getTs());

            MessageMeta meta = new MessageMeta(
                    res.getMid(),
                    res.getSeq(),
                    res.getTs(),
                    marker
            );

            boolean saved = lastRepo.save(chatId, meta);
            log.debug("Redis save result: {}", saved);

            log.info("Sent & saved: chatId={}, mid={}, seq={}, marker={}",
                    chatId, meta.mid(), meta.seq(), meta.marker());

        } catch (Exception e) {
            log.warn("sendMessage failed: chatId={}, marker={}, err={}, cause={}",
                    chatId, marker, e.toString(), e.getCause() != null ? e.getCause().toString() : "null");
        } finally {
            log.debug("sendMessage finished: chatId={}, marker={}", chatId, marker);
        }
    }

    public void sendAllHabitsList(long chatId, List<HabitDto> habits) {
        sendHabitList(chatId, habits,
                "Все твои привычки",
                "У тебя пока нет привычек. Начни с создания первой!");
    }

    // привычки на сегодня — HabitCheckinDto
    public void sendTodayHabitsList(long chatId, List<HabitCheckinDto> habits) {
        sendHabitCheckinList(chatId, habits,
                "Привычки на сегодня",
                "На сегодня привычек нет. Можно отдохнуть — или добавить что-то полезное 🙂");
    }

    // привычки на неделю — HabitCheckinDto
    public void sendWeekHabitsList(long chatId, List<HabitCheckinDto> habits) {
        sendHabitCheckinList(chatId, habits,
                "Привычки на неделю",
                "На эту неделю ещё нет привычек. Добавь хотя бы одну, чтобы разогнаться!");
    }

    private void sendHabitCheckinList(long chatId,
                                      List<HabitCheckinDto> habits,
                                      String title,
                                      String emptyMessage) {

        StringBuilder sb = new StringBuilder();

        sb.append("""
            **%s**

            """.formatted(title));

        if (habits == null || habits.isEmpty()) {
            sb.append(emptyMessage);
            var emptyBody = InlineKeyboardBuilder.create()
                    .text(sb.toString())
                    .format("markdown")
                    .addCallbackButton("➕ Создать привычку", Payload.HABITS_CREATE_NEW.key())
                    .addCallbackButton("🏠 В профиль",        Payload.HOME_PAGE.key())
                    .build();

            sendMessage(chatId, emptyBody, MessageMarker.HABIT_LIST);
            return;
        }

        sb.append("""
            Вот твои привычки. Нажми на любую, чтобы посмотреть детали
            и отметить выполнение.

            """);

        for (HabitCheckinDto habit : habits) {
            String description = habit.description() == null || habit.description().isBlank()
                    ? "_нет описания_"
                    : habit.description();

            String interval = habit.interval() == null
                    ? "не задана"
                    : habit.interval().getDisplayName();

            String goalDate = habit.goalDate() == null
                    ? "не задана"
                    : habit.goalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            String completed = habit.isCompleted() ? "✅" : "❌";

            sb.append("""
                %s **%s**
                Описание: %s
                Периодичность: %s
                Цель до: %s
                Выполнена: %s

                """.formatted(
                    habit.status().getEmoji(),
                    habit.title(),
                    description,
                    interval,
                    goalDate,
                    completed
            ));
        }

        sb.append("\n*Кликните на привычку, чтобы перейти к ней*");

        var body = InlineKeyboardBuilder.create()
                .text(sb.toString())
                .format("markdown");

        for (HabitCheckinDto habit : habits) {
            body.addCallbackButton(
                    habit.status().getEmoji() + " " + habit.title(),
                    Payload.HABITS_ID.key() + ":%s".formatted(habit.id())
            );
        }

        body.addCallbackButton("➕ Создать привычку", Payload.HABITS_CREATE_NEW.key());
        body.addCallbackButton("🏠 В профиль",        Payload.HOME_PAGE.key());

        sendMessage(chatId, body.build(), MessageMarker.HABIT_LIST);
    }

    public void sendHabitsStreaks(long chatId,
                                  HabitStatsDto stats,
                                  Map<Long, Integer> currentStreaks,
                                  Map<Long, Integer> longestStreaks) {

        StringBuilder sb = new StringBuilder();

        sb.append("""
                🔥 **Текущие серии по привычкам**

                Всего привычек: %d
                Активных привычек: %d

                """.formatted(
                stats.totalHabits(),
                stats.activeHabits()
        ));

        if (stats.totalHabits() == 0) {
            sb.append("Пока нет привычек для отслеживания серий. Создай первую привычку — и будем считать 🔥\n");
        } else {
            sb.append("Вот твои серии:\n\n");
        }

        var body = InlineKeyboardBuilder.create()
                .text(sb.toString())
                .format("markdown");

        // Кнопки по привычкам (если надо, можно дополнять)
        for (Map.Entry<Long, Integer> entry : currentStreaks.entrySet()) {
            Long habitId = entry.getKey();
            Integer current = entry.getValue();
            Integer longest = longestStreaks.getOrDefault(habitId, 0);

            body.addCallbackButton(
                    "🔥 " + current + " / 🏆 " + longest,
                    Payload.HABITS_ID.key() + ":%s".formatted(habitId)
            );
        }

        body.addCallbackButton("🏠 В профиль", Payload.HOME_PAGE.key());

        sendMessage(chatId, body.build(), MessageMarker.HABIT_LIST);
    }

    private void sendHabitList(long chatId,
                               List<HabitDto> habits,
                               String title,
                               String emptyMessage) {

        StringBuilder sb = new StringBuilder();

        sb.append("""
                **%s**

                """.formatted(title));

        if (habits == null || habits.isEmpty()) {
            sb.append(emptyMessage);
            var emptyBody = InlineKeyboardBuilder.create()
                    .text(sb.toString())
                    .format("markdown")
                    .addCallbackButton("➕ Создать привычку", Payload.HABITS_CREATE_NEW.key())
                    .addCallbackButton("🏠 В профиль",        Payload.HOME_PAGE.key())
                    .build();

            sendMessage(chatId, emptyBody, MessageMarker.HABIT_LIST);
            return;
        }

        sb.append("""
                Вот твои привычки. Нажми на любую, чтобы посмотреть детали
                и отметить выполнение.

                """);

        for (HabitDto habit : habits) {
            sb.append("""
                    %s **%s**
                    Описание: %s
                    Периодичность: %s
                    Цель до: %s

                    """.formatted(
                    habit.status().getEmoji(),
                    habit.title(),
                    habit.description() == null || habit.description().isBlank()
                            ? "_нет описания_"
                            : habit.description(),
                    habit.interval() == null ? "не задана" : habit.interval().getDisplayName(),
                    habit.goalDate() == null
                            ? "не задана"
                            : habit.goalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            ));
        }

        sb.append("\n*Кликните на привычку, чтобы перейти к ней*");

        var body = InlineKeyboardBuilder.create()
                .text(sb.toString())
                .format("markdown");

        for (HabitDto habit : habits) {
            body.addCallbackButton(
                    habit.status().getEmoji() + " " + habit.title(),
                    Payload.HABITS_ID.key() + ":%s".formatted(habit.id())
            );
        }

        body.addCallbackButton("➕ Создать привычку", Payload.HABITS_CREATE_NEW.key());
        body.addCallbackButton("🏠 В профиль",        Payload.HOME_PAGE.key());

        sendMessage(chatId, body.build(), MessageMarker.HABIT_LIST);
    }

    public void sendHabitTitleInput(long chatId) {
        String text = """
                Введите название привычки:
                """;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);

        sendMessage(chatId, body, MessageMarker.CHANGE_HABIT_TITLE);
    }

    public void sendHabitDescriptionInput(long chatId) {
        String text = """
                Введите краткое описание привычки:
                """;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);

        sendMessage(chatId, body, MessageMarker.CHANGE_HABIT_DESCRIPTION);
    }

    public void sendHabitIntervalInput(long chatId) {
        var body = InlineKeyboardBuilder.create()
                .text("""
                        **Периодичность привычки**

                        Выбери, как часто должна повторяться привычка:
                        """)
                .format("markdown")
                .addMessageButton(HabitInterval.EVERY_DAY.getDisplayName(), HabitInterval.EVERY_DAY.getDisplayName())
                .addMessageButton(HabitInterval.EVERY_WEEKDAY.getDisplayName(), HabitInterval.EVERY_WEEKDAY.getDisplayName())
                .addMessageButton(HabitInterval.EVERY_WEEKEND.getDisplayName(), HabitInterval.EVERY_WEEKEND.getDisplayName())
                .addMessageButton(HabitInterval.EVERY_WEEK.getDisplayName(), HabitInterval.EVERY_WEEK.getDisplayName())
                .addMessageButton(HabitInterval.EVERY_MONDAY.getDisplayName(), HabitInterval.EVERY_MONDAY.getDisplayName())
                .addMessageButton(HabitInterval.EVERY_TUESDAY.getDisplayName(), HabitInterval.EVERY_TUESDAY.getDisplayName())
                .addMessageButton(HabitInterval.EVERY_WEDNESDAY.getDisplayName(), HabitInterval.EVERY_WEDNESDAY.getDisplayName())
                .addMessageButton(HabitInterval.EVERY_THURSDAY.getDisplayName(), HabitInterval.EVERY_THURSDAY.getDisplayName())
                .addMessageButton(HabitInterval.EVERY_FRIDAY.getDisplayName(), HabitInterval.EVERY_FRIDAY.getDisplayName())
                .addMessageButton(HabitInterval.EVERY_SATURDAY.getDisplayName(), HabitInterval.EVERY_SATURDAY.getDisplayName())
                .addMessageButton(HabitInterval.EVERY_SUNDAY.getDisplayName(), HabitInterval.EVERY_SUNDAY.getDisplayName())
                .build();

        sendMessage(chatId, body, MessageMarker.CHANGE_HABIT_INTERVAL);
    }

    public Object createHabitCreateKeyboardBody(String title,
                                                String description,
                                                String interval,
                                                String goalDate) {

        return InlineKeyboardBuilder.create()
                .text("""
                    💪**Создание привычки.**
                    Создайте привычку, используя кнопки ниже.
                    Заполните название, описание, периодичность и дату цели —
                    после этого можно будет подтвердить создание.

                    Название: %s
                    Описание: %s
                    Периодичность: %s
                    Цель до: %s
                    """.formatted(
                        title == null || title.isBlank() ? "." : title,
                        description == null || description.isBlank() ? "." : description,
                        interval == null || interval.isBlank() ? "." : interval,
                        goalDate == null || goalDate.isBlank() ? "." : goalDate
                ))
                .format("markdown")
                .addCallbackButton("Изменить название",       Payload.HABITS_CHANGE_TITLE.key())
                .addCallbackButton("Изменить описание",       Payload.HABITS_CHANGE_DESCRIPTION.key())
                .addCallbackButton("Изменить периодичность",  Payload.HABITS_CHANGE_INTERVAL.key())
                .addCallbackButton("Изменить дату завершения",Payload.HABITS_CHANGE_GOAL_DATE.key())
                .addCallbackButton("Подтвердить создание",    Payload.HABITS_CREATE_CONFIRM.key())
                .addCallbackButton("Меню привычек",        Payload.HABIT_MENU.key())
                .addCallbackButton("Меню задач",        Payload.TASK_MENU.key())
                .addCallbackButton("Вернуться в главное меню",        Payload.HOME_PAGE.key())
                .build();
    }

    public void sendHabitCreateKeyboard(long chatId) {
        var body = InlineKeyboardBuilder.create()
                .text("""
                        💪**Создание привычки.**
                        Создайте привычку, используя кнопки ниже.
                        Заполните название, описание, периодичность и дату цели —
                        после этого можно будет подтвердить создание.
                        
                        Название: .
                        Описание: .
                        Периодичность: .
                        Цель до: .
                        """)
                .format("markdown")
                .addCallbackButton("Изменить название", Payload.HABITS_CHANGE_TITLE.key())
                .addCallbackButton("Изменить описание", Payload.HABITS_CHANGE_DESCRIPTION.key())
                .addCallbackButton("Изменить периодичность", Payload.HABITS_CHANGE_INTERVAL.key())
                .addCallbackButton("Изменить дату завершения", Payload.HABITS_CHANGE_GOAL_DATE.key())
                .addCallbackButton("Подтвердить создание", Payload.HABITS_CREATE_CONFIRM.key())
                .addCallbackButton("Меню привычек",        Payload.HABIT_MENU.key())
                .addCallbackButton("Меню задач",        Payload.TASK_MENU.key())
                .addCallbackButton("Вернуться в главное меню",        Payload.HOME_PAGE.key())
                .build();

        sendMessage(chatId, body, MessageMarker.CREATE_HABIT);
    }

    public void sendHabitGoalDateInput(long chatId) {
        String text = """
                Введите дату завершения привычки в формате dd.MM.yyyy:
                """;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);

        sendMessage(chatId, body, MessageMarker.CHANGE_HABIT_GOAL_DATE);
    }
}
