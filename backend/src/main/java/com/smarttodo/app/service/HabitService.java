package com.smarttodo.app.service;

import com.smarttodo.app.dto.HabitDto;
import com.smarttodo.app.entity.HabitInterval;
import com.smarttodo.app.entity.HabitStatus;

import java.util.List;

public class HabitService {

    HabitDto createHabit(Long userId, HabitDto createHabitDto) {
        return null;
    };

    void updateHabitStatus(Long habitId, HabitStatus newStatus) {
        return;
    };

    void markHabitAsArchived(Long habitId) {
        return;
    };

    void markHabitAsArchived(Long habitId) {
        return;
    };

    List<HabitDto> getAllHabits(Long userId) {
        return null;
    }

    List<HabitDto> getHabitForToday(Long userId) {
        return null;
    }

    List<HabitDto> getHabitForWeek(Long userId) {
        return null;
    }

    List<HabitDto> getUncompletedHabitsForToday(Long userId) {
        return null;
    }

    List<HabitDto> getUncompletedHabitsForWeek(Long userId) {
        return null;
    }

    void updateHabitInterval(Long userId, Long habitId, HabitInterval newInterval) {

    }

    HabitInterval getHabitInterval(Long userId, Long habitId) {
        return null;
    }
}
