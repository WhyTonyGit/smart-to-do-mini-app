package com.smarttodo.app.bot;

import com.smarttodo.app.client.MaxApi;
import com.smarttodo.app.dto.MessageMeta;
import com.smarttodo.app.dto.TaskDto;
import com.smarttodo.app.entity.TaskStatus;
import com.smarttodo.app.repository.LastActionRedisRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSender {

    private final MaxApi maxApi;        // postMessage возвращает Mono<SendMessageResult>
    private final LastActionRedisRepo lastRepo;   // синхронный репозиторий

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
                        Привет! Я твой помощник по самоорганизации. Помогу планировать день, вести задачи, отслеживать часы активности и формировать полезные привычки.

                        Что я делаю:
                        • Быстро добавляю и напоминаю о задачах
                        • Следую за прогрессом и показываю статистику выполнения
                        • Замеряю «часы активности» — когда ты реально делаешь дела
                        • Запускаю трекеры привычек и мотивирую не срываться
                        """)
                .format("markdown")
                .addCallbackButton("✅Задачи", "tasks-handler")
                .addCallbackButton("🗓️Привычки", "habit-handler")
                .addCallbackButton("⏰Напоминания", "notification-handler")
                .build();

        sendMessage(chatId, body, MessageMarker.WELCOME);
    }

    public void sendTaskKeyboard(long chatId) {
        var body = InlineKeyboardBuilder.create()
                .text("""
                      📝**Меню задач**
                      """)
                .format("markdown")
                .addCallbackButton("Задачи на сегодня", "tasks-get-today")
                .addCallbackButton("Задачи на неделю", "tasks-get-week")
                .addCallbackButton("Создать задачу", "tasks-create-new")
                .build();

        sendMessage(chatId, body, MessageMarker.TASK_MENU);
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
                      📝**Создание задачи...**
                      Создайте задачу используя кнопки ниже. 
                      Также вы можете использовать betta режим и описать задачу, которую хотите создать.
                      Обязательно укажите дату и время дэдлайна по задаче. После обработки поля ниже заполняться автоматически.
                      Обработка текста занимает 10 - 30 секунд.
                      Затем вы сможете с помощью кнопок отредактировать задачу.
                      Название: %s
                      Описание: %s
                      Дэдлайн: %s
                      """.formatted(title, description, deadline))
                .format("markdown")
                .addCallbackButton("Подтвердить создание", "tasks-create-confirm")
                .addCallbackButton("Вернуться в меню", "home-page")
                .addCallbackButton("Изменить название", "tasks-change-title")
                .addCallbackButton("Изменить описание", "tasks-change-description")
                .addCallbackButton("Изменить дэдлайн", "tasks-change-deadline")
                .build();
    }

    public void sendTaskCreateKeyboard(long chatId) {
        var body = InlineKeyboardBuilder.create()
                .text("""
                      📝**Создание задачи...**
                      Создайте задачу используя кнопки ниже. 
                      Также вы можете использовать betta режим и описать задачу, которую хотите создать.
                      Обязательно укажите дату и время дэдлайна по задаче. После обработки поля ниже заполняться автоматически.
                      Обработка текста занимает 10 - 30 секунд.
                      Затем вы сможете с помощью кнопок отредактировать задачу.
                      Название: ...
                      Описание: ...
                      Дэдлайн: ...
                      """)
                .format("markdown")
                .addCallbackButton("Подтвердить создание", "confirm")
                .addCallbackButton("Вернуться в меню", "home-page")
                .addCallbackButton("Изменить название", "tasks-change-title")
                .addCallbackButton("Изменить описание", "tasks-change-description")
                .addCallbackButton("Изменить дэдлайн", "tasks-change-deadline")
                .build();

        sendMessage(chatId, body, MessageMarker.CREATE_TASK);
    }

    public void sendHomePageKeyboard(long chatId) {
        var body = InlineKeyboardBuilder.create()
                .text("""
                      ⏩**Меню приложения**⏪
                      """)
                .format("markdown")
                .addCallbackButton("Все задачи", "tasks-get-all")
                .addCallbackButton("Задачи на сегодня", "tasks-get-today")
                .addCallbackButton("Задачи на неделю", "tasks-get-week")
                .addCallbackButton("Создать задачу", "tasks-create-new")
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
                .text(
                        """
                        %s **%s**
                        Описание: %s
                        Дэдлайн: %s
                        Приоритет: %s
                        Статус: %s
                        """.formatted(
                                task.status().getEmoji(),
                                task.title(),
                                task.description(),
                                TaskManager.formatLocalDateTime(task.deadline()),
                                task.priority().getDescription(),
                                task.status().getDescription()
                        ))
                .format("markdown")
                .addCallbackButton("Поставить статус \"В процессе\"", "tasks-set-status-in_progress:%s".formatted(task.id()))
                .addCallbackButton("Поставить статус \"Выполнена\"", "tasks-set-status-done:%s".formatted(task.id()))
                .addCallbackButton("Удалить задачу",  "tasks-delete:%s".formatted(task.id()))
                .build();

        sendMessage(chatId, body, MessageMarker.TASK_LIST);
    }

    private void sendTaskList(long  chatId, List<TaskDto> tasks, String title) {
        String text = """
                        **%s**
                        Кликнете на задачу, перейти к ней.
                        
                        """.formatted(title);
        StringBuilder sb = new StringBuilder(text);
        for (var task: tasks){
            if (task.status() == TaskStatus.DONE) {
                sb.append(
                        """
                        ~~%s %s~~
                        """.formatted(task.status().getEmoji(), task.title())
                );
            } else {
                sb.append(
                        """
                        %s %s
                        Описание: %s
                        Дэдлайн: %s
                        """.formatted(task.status().getEmoji(), task.title(), task.description(), TaskManager.formatLocalDateTime(task.deadline()))
                );
            }
        }
        var body = InlineKeyboardBuilder.create()
                .text(sb.toString())
                .format("markdown");
        for (var task : tasks) {
            body.addCallbackButton(task.status().getEmoji() + ' ' + task.title(), "task-id:%s".formatted(task.id()));
        }
        body.addCallbackButton("Создать задачу", "tasks-create-new");
        body.addCallbackButton("Вернуться в меню", "home-page");
        sendMessage(chatId, body.build(), MessageMarker.TASK_LIST);
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
            log.warn("sendMessage failed: chatId={}, marker={}, err={}", chatId, marker, e.toString());
        } finally {
            log.debug("sendMessage finished: chatId={}, marker={}", chatId, marker);
        }
    }
}
