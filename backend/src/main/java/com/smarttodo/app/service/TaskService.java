package com.smarttodo.app.service;

import com.smarttodo.app.dto.TaskDto;
import com.smarttodo.app.entity.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TaskService {

    TaskDto createTask(Long userId, TaskDto createTaskDto) {
        return null;
    };

    void updateTaskStatus(Long userId, Long taskId, TaskStatus newStatus) {
        return;
    };

    void markTaskAsCompleted(Long userId, Long taskId){
    };

    void markTaskAsUncompleted(Long userId, Long taskId){

    };

    List<TaskDto> getAllTasks(Long userId){
        return null;
    };

    List<TaskDto> getAllTasksForToday(Long userId) {
        return null;
    };

    List<TaskDto> getAllTasksForWeek(Long userId) {
        return null;
    };

    List<TaskDto> getUncompletedTasksForToday(Long userId) {
        return null;
    };

    List<TaskDto> getUncompletedTasksForWeek(Long userId) {
        return null;
    };

    TaskDto getTaskById(Long taskId){
        return null;
    };

    TaskDto updateTaskDuringDate(Long userId, Long taskId, LocalDateTime duringDate) {
        return null;
    };
}
