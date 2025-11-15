package com.smarttodo.app.service;

import com.smarttodo.app.bot.MessageSender;
import com.smarttodo.app.dto.HabitCheckinDto;
import com.smarttodo.app.dto.TaskDto;
import com.smarttodo.app.entity.TaskEntity;
import com.smarttodo.app.entity.TaskStatus;
import com.smarttodo.app.entity.UserEntity;
import com.smarttodo.app.repository.TaskRepository;
import com.smarttodo.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final MessageSender messageSender;
    private final HabitService habitService;

    public void sendRemindersToAllUsers() {
        List<Long> allChatIds = userRepository.findAll().stream()
                .map(UserEntity::getChatId)
                .toList();

        for (Long chatId : allChatIds) {
            try {
                checkUpcomingDeadlinesInHour(chatId);
                checkUpcomingDeadlinesInDay(chatId);
                sendTasksAndHabitsForToday(chatId);

                Thread.sleep(50);
            } catch (Exception ignored) {}
        }
    }

    public List<TaskDto> checkUpcomingDeadlinesInHour(Long chatId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);

        List<TaskEntity> upcomingTasks = taskRepository.findAllByChatId(chatId).stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> task.getDeadline().isAfter(now) && task.getDeadline().isBefore(oneHourLater))
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                .toList();

        List<TaskDto> taskDtos = upcomingTasks.stream()
                .map(this::toTaskDto)
                .toList();

        if (!taskDtos.isEmpty()) {
            messageSender.sendUpcomingTasks(chatId, taskDtos);
        }

        return taskDtos;
    }

    public List<TaskDto> checkUpcomingDeadlinesInDay(Long chatId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayLater = now.plusDays(1);

        List<TaskEntity> upcomingTasks = taskRepository.findAllByChatId(chatId).stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> task.getDeadline().isAfter(now) && task.getDeadline().isBefore(oneDayLater))
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                .toList();

        List<TaskDto> taskDtos = upcomingTasks.stream()
                .map(this::toTaskDto)
                .toList();

        if (!taskDtos.isEmpty()) {
            messageSender.sendUpcomingTasks(chatId, taskDtos);
        }

        return taskDtos;
    }

    public void sendTasksAndHabitsForToday(Long chatId) {
        List<TaskDto> todayTasks = taskRepository.findAllByChatId(chatId).stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> {
                    LocalDateTime deadline = task.getDeadline();
                    LocalDateTime todayStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
                    LocalDateTime tomorrowStart = todayStart.plusDays(1);
                    return deadline.isAfter(todayStart) && deadline.isBefore(tomorrowStart);
                })
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                .map(this::toTaskDto)
                .toList();

        List<HabitCheckinDto> todayHabits = habitService.getHabitsForToday(chatId);

        if (!todayTasks.isEmpty()) {
            messageSender.sendTodayTaskList(chatId, todayTasks);
        }

        if (!todayHabits.isEmpty()) {
            messageSender.sendTodayHabitsList(chatId, todayHabits);
        }
    }

    private TaskDto toTaskDto(TaskEntity entity) {
        return new TaskDto(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getDeadline(),
                entity.getCompletedAt()
        );
    }
}