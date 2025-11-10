package com.smarttodo.app.service;

import com.smarttodo.app.dto.HabitDto;
import com.smarttodo.app.entity.HabitCheckinEntity;
import com.smarttodo.app.entity.HabitEntity;
import com.smarttodo.app.entity.HabitInterval;
import com.smarttodo.app.entity.HabitStatus;
import com.smarttodo.app.entity.UserEntity;
import com.smarttodo.app.repository.HabitCheckinRepository;
import com.smarttodo.app.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitCheckinRepository habitCheckinRepository;
    private final UserService userService;

    @Transactional
    public HabitDto createHabit(Long userId, HabitDto createHabitDto) {
        UserEntity user = userService.getUserByChatId(userId);

        HabitEntity habit = new HabitEntity(user, createHabitDto.title());
        habit.setDescription(createHabitDto.description());
        habit.setStatus(HabitStatus.ARCHIVED);
        habit.setInterval(createHabitDto.interval());
        habit.setPriority(createHabitDto.priority());
        habit.setGoalDate(createHabitDto.goalDate());

        HabitEntity savedHabit = habitRepository.save(habit);
        return toDto(savedHabit);
    }

    @Transactional
    public void updateHabitStatus(Long habitId, HabitStatus newStatus) {
        HabitEntity habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Не найдена привычка с id: " + habitId));

        habit.setStatus(newStatus);
        habitRepository.save(habit);
    }

    @Transactional
    public void markHabitAsArchived(Long habitId) {
        updateHabitStatus(habitId, HabitStatus.ARCHIVED);
    }

    @Transactional(readOnly = true)
    public List<HabitDto> getAllHabits(Long userId) {
        return habitRepository.findAllByUser_Id(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HabitDto> getHabitForToday(Long userId) {
        LocalDate today = LocalDate.now();
        return habitRepository.findAllByUser_Id(userId).stream()
                .filter(habit -> isHabitDueToday(habit, today))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HabitDto> getHabitForWeek(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);

        return habitRepository.findAllByUser_Id(userId).stream()
                .filter(habit -> isHabitDueInPeriod(habit, today, weekEnd))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HabitDto> getUncompletedHabitsForToday(Long userId) {
        return getHabitForToday(userId).stream()
                .filter(habit -> !isHabitCompletedForDate(habit.id(), LocalDate.now()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HabitDto> getUncompletedHabitsForWeek(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);

        return getHabitForWeek(userId).stream()
                .filter(habit -> {
                    LocalDate habitDate = getNextDueDate(habit, today);
                    return !isHabitCompletedForDate(habit.id(), habitDate);
                })
                .toList();
    }

    @Transactional
    public void updateHabitInterval(Long userId, Long habitId, HabitInterval newInterval) {
        HabitEntity habit = getHabitByIdAndUserId(habitId, userId);
        habit.setInterval(newInterval);
        habitRepository.save(habit);
    }

    @Transactional(readOnly = true)
    public HabitInterval getHabitInterval(Long userId, Long habitId) {
        HabitEntity habit = getHabitByIdAndUserId(habitId, userId);
        return habit.getInterval();
    }

    @Transactional
    public void checkinHabit(Long habitId, LocalDate date) {
        HabitEntity habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Не найдена привычка с id: " + habitId));

        if (habitCheckinRepository.existsByHabit_IdAndDay(habitId, date)) {
            throw new IllegalArgumentException("У привычки уже есть день: " + date);
        }

        HabitCheckinEntity checkin = new HabitCheckinEntity(habit, date);
        habitCheckinRepository.save(checkin);
    }

    private HabitEntity getHabitByIdAndUserId(Long habitId, Long userId) {
        HabitEntity habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Не найдена привычка с id: " + habitId));

        if (!habit.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Привычка не принадлежит юзеру");
        }

        return habit;
    }

    private boolean isHabitDueToday(HabitEntity habit, LocalDate today) {
        return habit.getStatus() == HabitStatus.IN_PROGRESS &&
                (habit.getGoalDate() == null || !habit.getGoalDate().isBefore(today));
    }

    private boolean isHabitDueInPeriod(HabitEntity habit, LocalDate start, LocalDate end) {
        return habit.getStatus() == HabitStatus.IN_PROGRESS &&
                (habit.getGoalDate() == null ||
                        (!habit.getGoalDate().isBefore(start) && !habit.getGoalDate().isAfter(end)));
    }

    private boolean isHabitCompletedForDate(Long habitId, LocalDate date) {
        return habitCheckinRepository.existsByHabit_IdAndDay(habitId, date);
    }

    // Тут ещё нет механизма интервала привычки, надо потом это говно доделать
    private LocalDate getNextDueDate(HabitDto habit, LocalDate fromDate) {
        return fromDate;
    }

    private HabitDto toDto(HabitEntity entity) {
        return new HabitDto(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getInterval(),
                entity.getPriority(),
                entity.getGoalDate()
        );
    }
}
