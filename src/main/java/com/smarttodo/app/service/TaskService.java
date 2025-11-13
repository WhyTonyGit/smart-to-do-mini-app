package com.smarttodo.app.service;

import com.smarttodo.app.dto.TaskDto;
import com.smarttodo.app.entity.TaskEntity;
import com.smarttodo.app.entity.TaskStatus;
import com.smarttodo.app.entity.UserEntity;
import com.smarttodo.app.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    @Transactional
    public TaskDto createTask(Long chatId, TaskDto createTaskDto) {
        UserEntity user = userService.getUserByChatId(chatId);

        TaskEntity task = new TaskEntity(user, createTaskDto.title(), chatId);
        task.setDescription(createTaskDto.description());
        task.setStatus(TaskStatus.NEW);
        task.setPriority(createTaskDto.priority());
        task.setDeadline(createTaskDto.deadline());

        TaskEntity savedTask = taskRepository.save(task);
        return toDto(savedTask);
    }

    @Transactional
    public void updateTaskStatus(Long userId, Long taskId, TaskStatus newStatus) {
        TaskEntity task = getTaskByIdAndUserId(taskId, userId);
        task.setStatus(newStatus);

        if (newStatus == TaskStatus.DONE) {
            task.setCompletedAt(Instant.now());
        } else {
            task.setCompletedAt(null);
        }

        taskRepository.save(task);
    }

    @Transactional
    public void markTaskAsCompleted(Long userId, Long taskId) {
        updateTaskStatus(userId, taskId, TaskStatus.DONE);
    }

    @Transactional
    public void markTaskAsUncompleted(Long userId, Long taskId) {
        TaskEntity task = getTaskByIdAndUserId(taskId, userId);
        task.setStatus(TaskStatus.NEW);
        task.setCompletedAt(null);
        taskRepository.save(task);
    }

    @Transactional
    public void markTaskAsInProgress(Long userId, Long taskId) {
        TaskEntity task = getTaskByIdAndUserId(taskId, userId);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setCompletedAt(null);
        taskRepository.save(task);
    }


    @Transactional(readOnly = true)
    public List<TaskDto> getAllTasks(Long chatId) {
        return taskRepository.findAllByChatId(chatId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getAllTasksForToday(Long chatId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return taskRepository.findAllByChatIdAndDeadlineBetween(chatId, startOfDay, endOfDay).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getAllTasksForWeek(Long chatId) {
        LocalDateTime startOfWeek = LocalDate.now().atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusDays(7).with(LocalTime.MAX);

        return taskRepository.findAllByChatIdAndDeadlineBetween(chatId, startOfWeek, endOfWeek).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getUncompletedTasksForToday(Long chatId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return taskRepository.findAllByChatIdAndDeadlineBetween(chatId, startOfDay, endOfDay).stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getUncompletedTasksForWeek(Long chatId) {
        LocalDateTime startOfWeek = LocalDate.now().atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusDays(7).with(LocalTime.MAX);

        return taskRepository.findAllByChatIdAndDeadlineBetween(chatId, startOfWeek, endOfWeek).stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long taskId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Не найдена задача с id: " + taskId));
        return toDto(task);
    }

    @Transactional
    public TaskDto updateTaskDeadline(Long userId, Long taskId, LocalDateTime newDeadline) {
        TaskEntity task = getTaskByIdAndUserId(taskId, userId);
        task.setDeadline(newDeadline);
        TaskEntity updatedTask = taskRepository.save(task);
        return toDto(updatedTask);
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getOverdueTasks(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return taskRepository.findAllByUser_Id(userId).stream()
                .filter(task -> task.getDeadline() != null &&
                        task.getDeadline().isBefore(now) &&
                        task.getStatus() != TaskStatus.DONE)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasksByStatus(Long userId, TaskStatus status) {
        return taskRepository.findAllByChatIdAndStatus(userId, status).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasksWithoutDeadline(Long userId) {
        return taskRepository.findAllByUser_Id(userId).stream()
                .filter(task -> task.getDeadline() == null)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private TaskEntity getTaskByIdAndUserId(Long taskId, Long userId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + taskId));

        if (!task.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Task does not belong to user");
        }

        return task;
    }

    private TaskDto toDto(TaskEntity entity) {
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