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

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class HabitManager {

    private final HabitService habitService;
    private final PendingHabitRedisRepo habitRedisRepo;
    private final LastActionRedisRepo lastActionRepo;

    private final MessageSender messageSender;

    public void pickHabit(Update u) {
        Payload payload = Payload.from(u.getPayload());
        HabitDto habit = habitService.getHabitById(payload.extractId(u.getPayload()));

        switch (payload) {
            case HABITS_ID -> {
                break;
            }
            case HABITS_SET_STATUS_ARCHIVED -> {
                habitService.markHabitAsArchived(habit.id());
                messageSender.sendHabit(u.chatId(), habitService.getHabitById(payload.extractId(u.getPayload())));
            }
            case HABITS_SET_STATUS_COMPLETED -> {
                habitService.markHabitAsCompleted(habit.id());
                messageSender.sendHabit(u.chatId(), habitService.getHabitById(payload.extractId(u.getPayload())));
            }
            case HABITS_SET_STATUS_PAUSED -> {
                habitService.markHabitAsPaused(habit.id());
                messageSender.sendHabit(u.chatId(), habitService.getHabitById(payload.extractId(u.getPayload())));
            }
            case HABITS_SET_STATUS_IN_PROGRESS -> {
                habitService.markHabitAsInProgress(habit.id());
                messageSender.sendHabit(u.chatId(), habitService.getHabitById(payload.extractId(u.getPayload())));
            }
            case HABITS_MARK_AS_COMPLETED -> {
                habitService.uncheckinHabit(habit.id(), LocalDate.now());
                messageSender.sendHabit(u.chatId(), habitService.getHabitById(payload.extractId(u.getPayload())));
            }
            case HABITS_CHANGE_TITLE -> {
                messageSender.sendHabitTitleInput(habit.id());
            }
            case HABITS_CHANGE_DESCRIPTION -> {
                messageSender.sendHabitDescriptionInput(habit.id());
            }
            case HABITS_CHANGE_INTERVAL -> {
                messageSender.sendHabitIntervalInput(habit.id());
            }
            case HABITS_CHANGE_GOAL_DATE -> {
                messageSender.sendHabitGoalDateInput(habit.id());
            }
        }
    }
}
