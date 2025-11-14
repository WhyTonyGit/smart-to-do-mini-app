package com.smarttodo.app.service;

import com.smarttodo.app.bot.MessageSender;
import com.smarttodo.app.dto.HabitDto;
import com.smarttodo.app.dto.TaskDto;
import com.smarttodo.app.entity.HabitEntity;
import com.smarttodo.app.entity.TaskEntity;
import com.smarttodo.app.entity.TaskStatus;
import com.smarttodo.app.repository.HabitRepository;
import com.smarttodo.app.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReminderService {
    private final TaskRepository taskRepository;
    private final HabitRepository habitRepository;
    private final MessageSender messageSender;
    private final HabitService habitService;

    public List<TaskDto> checkUpcomingDeadlinesInHour() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);

        List<TaskEntity> upcomingTasks = taskRepository.findAll().stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> task.getDeadline().isAfter(now) && task.getDeadline().isBefore(oneHourLater))
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .toList();

        List<TaskDto> taskDtos = upcomingTasks.stream()
                .map(this::toTaskDto)
                .collect(Collectors.toList());

        if (!taskDtos.isEmpty()) {
            messageSender.sendUpcomingTasks(taskDtos);
        }

        return taskDtos;
    }

    public List<TaskDto> checkUpcomingDeadlinesInDay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayLater = now.plusDays(1);

        List<TaskEntity> upcomingTasks = taskRepository.findAll().stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> task.getDeadline().isAfter(now) && task.getDeadline().isBefore(oneDayLater))
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .toList();

        List<TaskDto> taskDtos = upcomingTasks.stream()
                .map(this::toTaskDto)
                .collect(Collectors.toList());

        if (!taskDtos.isEmpty()) {
            messageSender.sendUpcomingTasks(taskDtos);
        }

        return taskDtos;
    }

    public List<TaskDto> sendTasksAndHabitsForToday() {
        List<TaskDto> todayTasks = taskRepository.findAll().stream()
                .filter(task -> task.getDeadline() != null)
                .filter(task -> {
                    LocalDateTime deadline = task.getDeadline();
                    LocalDateTime todayStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
                    LocalDateTime tomorrowStart = todayStart.plusDays(1);
                    return deadline.isAfter(todayStart) && deadline.isBefore(tomorrowStart);
                })
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .map(this::toTaskDto)
                .collect(Collectors.toList());

        List<HabitEntity> todayHabits = habitRepository.findAll().stream()
                .filter(habit -> habitService.isHabitDueToday(habit, LocalDate.now()))
                .toList();

        List<HabitDto> habitDtos = todayHabits.stream()
                .map(habitService::toDto)
                .collect(Collectors.toList());

        messageSender.sendTodayTasksList(todayTasks);
        messageSender.sendTodayHabitsList(habitDtos);

        return todayTasks;
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
