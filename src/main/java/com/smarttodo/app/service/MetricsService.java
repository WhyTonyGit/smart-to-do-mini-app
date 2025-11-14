package com.smarttodo.app.service;

import com.smarttodo.app.client.MaxApi;
import com.smarttodo.app.dto.HabitStatsDto;
import com.smarttodo.app.dto.TaskStatsDto;
import com.smarttodo.app.dto.WeeklySummaryDto;
import com.smarttodo.app.entity.*;
import com.smarttodo.app.repository.HabitCheckinRepository;
import com.smarttodo.app.repository.HabitRepository;
import com.smarttodo.app.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetricsService {
    private final HabitRepository habitRepository;
    private final HabitCheckinRepository habitCheckinRepository;
    private final TaskRepository taskRepository;
    private final MaxApi maxApi;

    @Transactional(readOnly = true)
    public HabitStatsDto getHabitStats(Long chatId, LocalDate startDate, LocalDate endDate) {
        List<HabitEntity> habits = habitRepository.findAllByChatId(chatId);

        int totalHabits = habits.size();
        int activeHabits = (int) habits.stream()
                .filter(h -> h.getStatus() == HabitStatus.IN_PROGRESS)
                .count();

        Map<Long, Double> completionRates = habits.stream()
                .collect(Collectors.toMap(
                        HabitEntity::getId,
                        habit -> calculateHabitCompletionRate(habit, startDate, endDate)
                ));

        Map<Long, Integer> longestStreaks = habits.stream()
                .collect(Collectors.toMap(
                        HabitEntity::getId,
                        habit -> calculateLongestStreak(habit.getId())
                ));

        Map<Long, Integer> currentStreaks = habits.stream()
                .collect(Collectors.toMap(
                        HabitEntity::getId,
                        habit -> calculateCurrentStreak(habit.getId())
                ));

        return new HabitStatsDto(
                totalHabits,
                activeHabits,
                completionRates,
                longestStreaks,
                currentStreaks,
                startDate,
                endDate
        );
    }

    @Transactional(readOnly = true)
    public TaskStatsDto getTaskStats(Long chatId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<TaskEntity> tasksInPeriod = taskRepository.findAllByChatIdAndDeadlineBetween(chatId, start, end);

        long totalTasks = tasksInPeriod.size();
        long completedTasks = tasksInPeriod.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .count();
        long overdueTasks = tasksInPeriod.stream()
                .filter(t -> t.getDeadline() != null &&
                        t.getDeadline().isBefore(LocalDateTime.now()) &&
                        t.getStatus() != TaskStatus.COMPLETED)
                .count();

        Map<Priority, Long> tasksByPriority = tasksInPeriod.stream()
                .collect(Collectors.groupingBy(TaskEntity::getPriority, Collectors.counting()));

        double avgCompletionTime = tasksInPeriod.stream()
                .filter(t -> t.getCompletedAt() != null && t.getCreatedAt() != null)
                .mapToLong(t -> t.getCompletedAt().getEpochSecond() - t.getCreatedAt().getEpochSecond())
                .average()
                .orElse(0.0);

        return new TaskStatsDto(
                totalTasks,
                completedTasks,
                overdueTasks,
                tasksByPriority,
                avgCompletionTime,
                startDate,
                endDate
        );
    }

    @Transactional(readOnly = true)
    public WeeklySummaryDto getWeeklySummary(Long chatId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));

        HabitStatsDto habitStats = getHabitStats(chatId, weekStart, weekEnd);
        TaskStatsDto taskStats = getTaskStats(chatId, weekStart, weekEnd);

        Map<java.time.DayOfWeek, Long> activeDays = getActiveDays(chatId, weekStart, weekEnd);

        return new WeeklySummaryDto(habitStats, taskStats, activeDays, weekStart, weekEnd);
    }

    public double calculateHabitCompletionRate(HabitEntity habit, LocalDate start, LocalDate end) {
        if (habit.getStatus() != HabitStatus.IN_PROGRESS) return 0.0;

        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        long checkedDays = habitCheckinRepository.findAllByHabit_IdAndDayBetween(habit.getId(), start, end)
                .size();

        return totalDays > 0 ? (double) checkedDays / totalDays * 100 : 0.0;
    }

    public int calculateCurrentStreak(Long habitId) {
        LocalDate currentDate = LocalDate.now();
        int streak = 0;

        while (habitCheckinRepository.existsByHabit_IdAndDay(habitId, currentDate)) {
            streak++;
            currentDate = currentDate.minusDays(1);
        }

        return streak;
    }

    public int calculateLongestStreak(Long habitId) {
        List<HabitCheckinEntity> checkins = habitCheckinRepository
                .findAllByHabit_IdOrderByDayAsc(habitId);

        if (checkins.isEmpty()) {
            return 0;
        }

        int longestStreak = 0;
        int currentStreak = 1;
        LocalDate previousDate = checkins.getFirst().getDay();

        for (int i = 1; i < checkins.size(); i++) {
            LocalDate currentDate = checkins.get(i).getDay();

            if (previousDate.plusDays(1).equals(currentDate)) {
                currentStreak++;
            } else {
                longestStreak = Math.max(longestStreak, currentStreak);
                currentStreak = 1;
            }

            previousDate = currentDate;
        }

        return Math.max(longestStreak, currentStreak);
    }

    public Map<DayOfWeek, Long> getActiveDays(Long chatId, LocalDate start, LocalDate end) {
        List<TaskEntity> tasks = taskRepository.findAllByChatIdAndDeadlineBetween(
                chatId, start.atStartOfDay(), end.atTime(23, 59, 59));

        return tasks.stream()
                .filter(t -> t.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCompletedAt().atZone(ZoneId.systemDefault()).getDayOfWeek(),
                        Collectors.counting()
                ));
    }
}
