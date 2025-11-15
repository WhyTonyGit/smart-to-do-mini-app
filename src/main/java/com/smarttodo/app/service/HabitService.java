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
    public HabitCheckinDto getHabitCheckinDtoById(Long habitId) {
        LocalDate today = LocalDate.now();

        HabitEntity habit = habitRepository.findById(habitId).orElseThrow();
        boolean isCompleted = isHabitCompletedForDate(habit.getId(), today);

        return toCheckinDto(habit, today, isCompleted, isCompleted);
    }

    @Transactional(readOnly = true)
    public List<HabitDto> getAllHabits(Long chatId) {
        return habitRepository.findAllByChatId(chatId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HabitDto> getHabitsForToday(Long chatId) {
        LocalDate today = LocalDate.now();

//        return habitRepository.findAllByChatId(chatId).stream()
//                .filter(habit -> isHabitDueToday(habit, today))
//                .map(habit -> {
//                    boolean isCompleted = isHabitCompletedForDate(habit.getId(), today);
//                    return toCheckinDto(habit, today, isCompleted, isCompleted); // второй isCompleted - заглушка на isCompletedOnTime
//                })
//                .toList();

        return habitRepository.findAllByChatId(chatId).stream()
                .filter(habit -> isHabitDueToday(habit, today))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HabitDto> getHabitsForWeek(Long chatId) {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);

//        return habitRepository.findAllByChatId(chatId).stream()
//                .flatMap(habit -> start.datesUntil(end.plusDays(1))
//                        .filter(day -> isHabitDueToday(habit, day))
//                        .map(day -> {
//                            boolean isCompleted = isHabitCompletedForDate(habit.getId(), day);
//                            return toCheckinDto(habit, day, isCompleted, isCompleted); // второй isCompleted - заглушка isCompletedOnTime
//                        })
//                )
//                .toList();

        return habitRepository.findAllByChatId(chatId).stream()
                .filter(habit -> isHabitDueInPeriod(habit, today, weekEnd))
                .map(this::toDto)
                .toList();
    }

//    @Transactional(readOnly = true)
//    public List<HabitCheckinDto> getUncompletedHabitsForToday(Long chatId) {
//        return getHabitsForToday(chatId).stream()
//                .filter(habit -> !habit.isCompleted())
//                .toList();
//    }
//
//    @Transactional(readOnly = true)
//    public List<HabitCheckinDto> getUncompletedHabitsForWeek(Long chatId) {
//        return getHabitsForWeek(chatId).stream()
//                .filter(habit -> !habit.isCompleted())
//                .toList();
//    }

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

    @Transactional
    public void uncheckinHabit(Long habitId, LocalDate date) {
        if (!habitCheckinRepository.existsByHabit_IdAndDay(habitId, date)) {
            throw new IllegalArgumentException("У привычки нет выполнения на дату: " + date);
        }

        habitCheckinRepository.deleteByHabit_IdAndDay(habitId, date);
    }

    private boolean isHabitDueToday(HabitEntity habit, LocalDate today) {
        if (habit.getStatus() != HabitStatus.IN_PROGRESS) {
            return false;
        }

        if (habit.getGoalDate() != null && habit.getGoalDate().isBefore(today)) {
            return false;
        }

        HabitInterval interval = habit.getInterval();
        if (interval == null) {
            return true;
        }
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        return switch (interval) {
            case EVERY_DAY -> true;
            case EVERY_WEEK -> dayOfWeek == DayOfWeek.MONDAY;
            case EVERY_WEEKEND -> dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
            case EVERY_WEEKDAY -> dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
            case EVERY_SUNDAY -> dayOfWeek == DayOfWeek.SUNDAY;
            case EVERY_MONDAY -> dayOfWeek == DayOfWeek.MONDAY;
            case EVERY_TUESDAY -> dayOfWeek == DayOfWeek.TUESDAY;
            case EVERY_WEDNESDAY -> dayOfWeek == DayOfWeek.WEDNESDAY;
            case EVERY_THURSDAY -> dayOfWeek == DayOfWeek.THURSDAY;
            case EVERY_FRIDAY -> dayOfWeek == DayOfWeek.FRIDAY;
            case EVERY_SATURDAY -> dayOfWeek == DayOfWeek.SATURDAY;
        };
    }

    private boolean isHabitDueInPeriod(HabitEntity habit, LocalDate start, LocalDate end) {
        return start.datesUntil(end.plusDays(1)).anyMatch(day -> isHabitDueToday(habit, day));
    }

    private boolean isHabitCompletedForDate(Long habitId, LocalDate date) {
        return habitCheckinRepository.existsByHabit_IdAndDay(habitId, date);
    }

    private LocalDate getNextDueDate(HabitDto habit, LocalDate fromDate) {
        HabitInterval interval = habit.interval();
        if (interval == null) {
            return fromDate;
        }

        return switch (interval) {
            case EVERY_DAY -> fromDate;
            case EVERY_WEEK -> fromDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            case EVERY_SUNDAY -> fromDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            case EVERY_MONDAY -> fromDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            case EVERY_TUESDAY -> fromDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
            case EVERY_WEDNESDAY -> fromDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
            case EVERY_THURSDAY -> fromDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));
            case EVERY_FRIDAY -> fromDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
            case EVERY_SATURDAY -> fromDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

            case EVERY_WEEKEND -> {
                DayOfWeek dow = fromDate.getDayOfWeek();
                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                    yield fromDate;
                } else {
                    yield fromDate.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
                }
            }

            case EVERY_WEEKDAY -> {
                DayOfWeek dow = fromDate.getDayOfWeek();
                if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                    yield fromDate;
                } else {
                    yield fromDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
                }
            }
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

    private HabitCheckinDto toCheckinDto(HabitEntity habit, LocalDate day, boolean isCompleted, boolean isCompletedOnTime) {
        return new HabitCheckinDto(
                habit.getId(),
                habit.getTitle(),
                habit.getDescription(),
                habit.getStatus(),
                habit.getInterval(),
                habit.getPriority(),
                day,
                habit.getGoalDate(),
                isCompleted,
                isCompletedOnTime
        );
    }
}
