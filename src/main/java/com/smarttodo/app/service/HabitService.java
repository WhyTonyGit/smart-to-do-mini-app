package com.smarttodo.app.service;

import com.smarttodo.app.dto.HabitCheckinDto;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitCheckinRepository habitCheckinRepository;
    private final UserService userService;

    @Transactional
    public HabitDto createHabit(Long chatId, HabitDto createHabitDto) {
        UserEntity user = userService.getUserByChatId(chatId);

        HabitEntity habit = new HabitEntity(chatId, user, createHabitDto.title());
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

    @Transactional
    public void markHabitAsInProgress(Long habitId) {
        updateHabitStatus(habitId, HabitStatus.IN_PROGRESS);
    }

    @Transactional
    public void markHabitAsPaused(Long habitId) {
        updateHabitStatus(habitId, HabitStatus.PAUSED);
    }

    @Transactional
    public void markHabitAsCompleted(Long habitId) {
        updateHabitStatus(habitId, HabitStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public HabitDto getHabitById(Long habitId) {
        HabitEntity habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Не найдена привычка с id: " + habitId));
        return toDto(habit);
    }

    @Transactional(readOnly = true)
    public List<HabitDto> getAllHabits(Long chatId) {
        return habitRepository.findAllByChatId(chatId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HabitCheckinDto> getHabitsForToday(Long chatId) {
        LocalDate today = LocalDate.now();

        return habitRepository.findAllByChatId(chatId).stream()
                .filter(habit -> isHabitDueToday(habit, today))
                .map(habit -> {
                    boolean completedOnTime = habitCheckinRepository.existsByHabit_IdAndDay(habit.getId(), today);
                    return toCheckinDto(habit, today, completedOnTime);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HabitCheckinDto> getHabitsForWeek(Long chatId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);

        return habitRepository.findAllByChatId(chatId).stream()
                .flatMap(habit -> start.datesUntil(end.plusDays(1))
                        .filter(day -> isHabitDueToday(habit, day))
                        .map(day -> {
                            boolean completedOnTime = habitCheckinRepository.existsByHabit_IdAndDay(habit.getId(), day);
                            return toCheckinDto(habit, day, completedOnTime);
                        })
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HabitCheckinDto> getUncompletedHabitsForToday(Long chatId) {
        return getHabitsForToday(chatId).stream()
                .filter(habit -> !habit.completedOnTime())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HabitCheckinDto> getUncompletedHabitsForWeek(Long chatId) {
        return getHabitsForWeek(chatId).stream()
                .filter(habit -> !habit.completedOnTime())
                .toList();
    }

    @Transactional
    public void updateHabitInterval(Long habitId, HabitInterval newInterval) {
        HabitEntity habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Не найдена привычка с id: " + habitId));
        habit.setInterval(newInterval);
        habitRepository.save(habit);
    }

    @Transactional(readOnly = true)
    public HabitInterval getHabitInterval(Long habitId) {
        HabitEntity habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new IllegalArgumentException("Не найдена привычка с id: " + habitId));
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

    private LocalDate getNextDueDate(HabitDto habit, LocalDate fromDate) {
        if (habit.interval() == null) {
            return fromDate;
        }

        return switch (habit.interval()) {
            case EVERY_DAY -> fromDate;
            case EVERY_WEEK -> fromDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            case EVERY_MONTH -> fromDate.with(TemporalAdjusters.firstDayOfNextMonth());
        };
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

    private HabitCheckinDto toCheckinDto(HabitEntity habit, LocalDate day, boolean completedOnTime) {
        return new HabitCheckinDto(
                habit.getId(),
                habit.getTitle(),
                habit.getDescription(),
                habit.getStatus(),
                habit.getInterval(),
                habit.getPriority(),
                day,
                completedOnTime
        );
    }
}
