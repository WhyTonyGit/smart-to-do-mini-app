package com.smarttodo.app.bot;

import com.smarttodo.app.dto.HabitDto;
import com.smarttodo.app.dto.TaskDto;
import com.smarttodo.app.entity.Update;
import com.smarttodo.app.repository.LastActionRedisRepo;
import com.smarttodo.app.repository.PendingHabitRedisRepo;
import com.smarttodo.app.service.HabitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HabitManager {

    private final HabitService habitService;
    private final PendingHabitRedisRepo habitRedisRepo;
    private final LastActionRedisRepo lastActionRepo;

    public void pickHabit(Update u) {
        Payload payload = Payload.from(u.getPayload());
        HabitDto task = habitService.getHabitById(payload.extractId(u.getPayload()));

        switch (payload) {
            case HABITS_ID -> {
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
            }
        }

        messageSender.sendTask(u.chatId(), taskService.getTaskById(payload.extractId(u.getPayload())));
    }
}
