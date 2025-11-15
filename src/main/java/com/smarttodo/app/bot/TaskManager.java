package com.smarttodo.app.bot;

import com.smarttodo.app.client.MaxApi;
import com.smarttodo.app.dto.MessageMeta;
import com.smarttodo.app.dto.TaskDto;
import com.smarttodo.app.entity.Update;
import com.smarttodo.app.entity.Priority;
import com.smarttodo.app.entity.TaskStatus;
import com.smarttodo.app.llm.NlpService;
import com.smarttodo.app.llm.dto.ParsedTask;
import com.smarttodo.app.repository.LastActionRedisRepo;
import com.smarttodo.app.repository.PendingTaskRedisRepo;
import com.smarttodo.app.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskManager {

    private final TaskService taskService;
    private final PendingTaskRedisRepo taskRedisRepo;
    private final LastActionRedisRepo lastActionRepo;

    private final MessageSender messageSender;
    private final NlpService nlp;
    private final MaxApi maxApi;

    public void parseTextWithLlm(Update u) {

        String mid = lastActionRepo.get(u.chatId()).map(MessageMeta::mid).orElse(null);

        String text = u.getText();
        if (text != null && !text.isBlank()) {
            try {
                var parsed = nlp.parseText(text).block(java.time.Duration.ofSeconds(60));
                if (parsed == null || parsed.tasks() == null || parsed.tasks().isEmpty()) {
                    messageSender.sendText(u.chatId(), "Не смог разобрать задачу. Сформулируй чуть яснее.");
                    return;
                }

                ParsedTask task = parsed.tasks().getFirst();

                TaskDto curTask = new TaskDto(
                        null,
                        task.title() == null ? "..." : task.title(),
                        task.description() == null ? "..." : task.description(),
                        null,
                        Priority.LOW,
                        parseStringToLocalDateTime(task.datetime()),
                        null
                );
                taskRedisRepo.save(u.chatId(), curTask);

                maxApi.editMessage(mid, messageSender.createTaskCreateKeyboardBody(
                        curTask.title(),
                        curTask.description(),
                        formatLocalDateTime(curTask.deadline())
                )).block();

            } catch (IllegalArgumentException e) {
                messageSender.sendText(u.chatId(), "Упс, модель не ответила вовремя. Попробуем ещё раз позже. %s".formatted(e.getMessage()));
                return;
            }
        }
    }

    public void createTask(Update u) {

        taskRedisRepo.save(u.chatId(), new TaskDto(
                null,
                "...",
                "...",
                TaskStatus.UNCOMPLETED,
                Priority.LOW,
                null,
                null
        ));

        messageSender.sendTaskCreateKeyboard(u.chatId());
    }

    public void confirmTaskCreating(Update u) {
        Optional<TaskDto> task = taskRedisRepo.get(u.chatId());

        if (task.isEmpty()) {
            messageSender.sendText(u.chatId(), "Срок создания по этой задаче истек, создайте заново.");
            return;
        }

        if (taskService.createTask(u.chatId(), task.get()) !=  null) {
            taskRedisRepo.delete(u.chatId());
            messageSender.sendText(u.chatId(), "Задача создана.");
        }
    }

    public void changeTaskTitle(Update u) {

        String text = u.getText();
        if (text != null && !text.isBlank()) {
           Optional<TaskDto> task = taskRedisRepo.get(u.chatId());

           TaskDto prevTask = task.orElse(null);
           if (prevTask == null) {
               prevTask = new TaskDto(
                       null,
                       "...",
                       "...",
                       TaskStatus.UNCOMPLETED,
                       Priority.LOW,
                       null,
                       null
               );
           }

           TaskDto newTask = new TaskDto(
                   prevTask.id(),
                   text,
                   prevTask.description(),
                   prevTask.status(),
                   prevTask.priority(),
                   prevTask.deadline(),
                   prevTask.completedAt()
           );

           taskRedisRepo.save(u.chatId(), newTask);

           maxApi.postMessage(u.chatId(), messageSender.createTaskCreateKeyboardBody(
                   newTask.title(),
                   newTask.description(),
                   formatLocalDateTime(newTask.deadline())
           )).block();
        }
    }

    public void changeTaskDescription(Update u) {

        String text = u.getText();
        if (text != null && !text.isBlank()) {
            Optional<TaskDto> task = taskRedisRepo.get(u.chatId());

            TaskDto prevTask = task.orElse(null);
            if (prevTask == null) {
                prevTask = new TaskDto(
                        null,
                        "...",
                        "...",
                        TaskStatus.UNCOMPLETED,
                        Priority.LOW,
                        null,
                        null
                );
            }

            TaskDto newTask = new TaskDto(
                    prevTask.id(),
                    prevTask.title(),
                    text,
                    prevTask.status(),
                    prevTask.priority(),
                    prevTask.deadline(),
                    prevTask.completedAt()
            );

            taskRedisRepo.save(u.chatId(), newTask);

            maxApi.postMessage(u.chatId(), messageSender.createTaskCreateKeyboardBody(
                    newTask.title(),
                    newTask.description(),
                    formatLocalDateTime(newTask.deadline())
            )).block();
        }
    }

    public void changeTaskDeadline(Update u) {

        String text = u.getText();
        if (text != null && !text.isBlank()) {
            Optional<TaskDto> task = taskRedisRepo.get(u.chatId());

            TaskDto prevTask = task.orElse(null);
            if (prevTask == null) {
                prevTask = new TaskDto(
                        null,
                        "...",
                        "...",
                        TaskStatus.UNCOMPLETED,
                        Priority.LOW,
                        null,
                        null
                );
            }

            try {
                TaskDto newTask = new TaskDto(
                        prevTask.id(),
                        prevTask.title(),
                        prevTask.description(),
                        prevTask.status(),
                        prevTask.priority(),
                        parseStringToLocalDateTime(text),
                        prevTask.completedAt()
                );

                taskRedisRepo.save(u.chatId(), newTask);

                maxApi.postMessage(u.chatId(), messageSender.createTaskCreateKeyboardBody(
                        newTask.title(),
                        newTask.description(),
                        formatLocalDateTime(newTask.deadline())
                )).block();

            } catch (DateTimeParseException e) {
                messageSender.sendText(u.chatId(), "Неверный формат даты, проверьте введенные данные.");
            }
        }
    }

    public void getTodayTaskList(Update u) {
        List<TaskDto> taskList = taskService.getAllTasksForToday(u.chatId());
        taskList.sort(Comparator.comparing(TaskDto::status).thenComparing(TaskDto::deadline).thenComparing(TaskDto::priority));
        if (taskList.isEmpty()) {
            messageSender.sendText(u.chatId(), "На сегодня нет задач.");
            return;
        }

        messageSender.sendTodayTaskList(u.chatId(), taskList);
    }

    public void getTomorrowTaskList(Update u) {
        List<TaskDto> taskList = taskService.getAllTasksForTomorrow(u.chatId());
        taskList.sort(Comparator.comparing(TaskDto::status).thenComparing(TaskDto::deadline).thenComparing(TaskDto::priority));
        if (taskList.isEmpty()) {
            messageSender.sendText(u.chatId(), "На завтра нет задач.");
            return;
        }

        messageSender.sendTodayTaskList(u.chatId(), taskList);
    }

    public void getWeekTaskList(Update u) {
        List<TaskDto> taskList = taskService.getAllTasksForWeek(u.chatId());
        taskList.sort(Comparator.comparing(TaskDto::status).thenComparing(TaskDto::deadline).thenComparing(TaskDto::priority));
        if (taskList.isEmpty()) {
            messageSender.sendText(u.chatId(), "На эту неделю нет задач.");
            return;
        }

        messageSender.sendWeekTaskList(u.chatId(), taskList);
    }

    public void getAllTaskList(Update u) {
        List<TaskDto> taskList = taskService.getAllTasks(u.chatId()).stream()
                .filter(task -> task.status() != TaskStatus.COMPLETED)
                .sorted(Comparator.comparing(TaskDto::status)
                        .thenComparing(TaskDto::deadline)
                        .thenComparing(TaskDto::priority))
                .toList();
        log.info("Все задачи {}", taskList);
        if (taskList.isEmpty()) {
            messageSender.sendText(u.chatId(), "Задачи отсутствуют.");
            return;
        }

        messageSender.sendAllTaskList(u.chatId(), taskList);
    }

    public void pickTask(Update u) {
        Payload payload = Payload.from(u.getPayload());
        TaskDto task = taskService.getTaskById(payload.extractId(u.getPayload()));

        switch (payload) {
            case TASKS_ID -> {
                break;
            }
            case TASKS_SET_STATUS_UNCOMPLETED -> {
                taskService.markTaskAsUncompleted(task.id());
            }
            case TASKS_SET_STATUS_IN_PROGRESS -> {
                taskService.markTaskAsInProgress(task.id());
            }
            case TASKS_SET_STATUS_COMPLETED -> {
                taskService.markTaskAsCompleted(task.id());
            }
            case TASKS_DELETE -> {
                taskService.deleteTask(task.id());
                messageSender.sendText(u.chatId(), "Задача удалена");
            }
        }

        messageSender.sendTask(u.chatId(), taskService.getTaskById(payload.extractId(u.getPayload())));
    }

    public static String formatLocalDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        if (dateTime == null) {
            return "...";
        }
        return dateTime.format(formatter);
    }

    public static LocalDateTime parseStringToLocalDateTime(String dateTimeString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        try {
            return LocalDateTime.parse(dateTimeString, formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверный формат даты и времени: " + dateTimeString);
        }
    }
}
