package com.smarttodo.app.service;

import com.smarttodo.app.client.MaxApi;
import com.smarttodo.app.dto.HabitStatsDto;
import com.smarttodo.app.dto.TaskStatsDto;
import com.smarttodo.app.dto.WeeklySummaryDto;
import com.smarttodo.app.entity.HabitStatus;
import com.smarttodo.app.entity.Priority;
import com.smarttodo.app.entity.HabitEntity;
import com.smarttodo.app.entity.TaskEntity;
import com.smarttodo.app.entity.TaskStatus;
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
    public HabitStatsDto getHabitStats(Long userId, LocalDate startDate, LocalDate endDate) {
        List<HabitEntity> habits = habitRepository.findAllByUser_Id(userId);

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
    public TaskStatsDto getTaskStats(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<TaskEntity> tasksInPeriod = taskRepository.findAllByUser_IdAndDeadlineBetween(userId, start, end);

        long totalTasks = tasksInPeriod.size();
        long completedTasks = tasksInPeriod.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();
        long overdueTasks = tasksInPeriod.stream()
                .filter(t -> t.getDeadline() != null &&
                        t.getDeadline().isBefore(LocalDateTime.now()) &&
                        t.getStatus() != TaskStatus.DONE)
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
    public WeeklySummaryDto getWeeklySummary(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));

        HabitStatsDto habitStats = getHabitStats(userId, weekStart, weekEnd);
        TaskStatsDto taskStats = getTaskStats(userId, weekStart, weekEnd);

        Map<java.time.DayOfWeek, Long> activeDays = getActiveDays(userId, weekStart, weekEnd);

        return new WeeklySummaryDto(habitStats, taskStats, activeDays, weekStart, weekEnd);
    }

    @Transactional
    public void checkAndNotifyAchievements(Long userId) {
        List<HabitEntity> activeHabits = habitRepository.findAllByUser_IdAndStatus(userId, HabitStatus.IN_PROGRESS);

        for (HabitEntity habit : activeHabits) {
            int currentStreak = calculateCurrentStreak(habit.getId());
            notifyStreakMilestones(userId, habit, currentStreak);
        }

        checkWeeklyAchievements(userId);
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
        // Пока что хз, как реализовать
        return 0;
    }

    public Map<DayOfWeek, Long> getActiveDays(Long userId, LocalDate start, LocalDate end) {
        List<TaskEntity> tasks = taskRepository.findAllByUser_IdAndDeadlineBetween(
                userId, start.atStartOfDay(), end.atTime(23, 59, 59));

        return tasks.stream()
                .filter(t -> t.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCompletedAt().atZone(ZoneId.systemDefault()).getDayOfWeek(),
                        Collectors.counting()
                ));
    }

    public void notifyStreakMilestones(Long userId, HabitEntity habit, int currentStreak) {
        if (currentStreak == 7) {
            maxApi.sendText(userId,
                    "🎉 Отличный результат! Вы выполняете привычку \"" + habit.getTitle() +
                            "\" уже 7 дней подряд! Так держать!");
        } else if (currentStreak == 30) {
            maxApi.sendText(userId,
                    "🏆 Потрясающе! 30 дней с привычкой \"" + habit.getTitle() +
                            "\"! Вы формируете устойчивую привычку!");
        } else if (currentStreak % 100 == 0 && currentStreak > 0) {
            maxApi.sendText(userId,
                    "🌟 Невероятно! Целых " + currentStreak + " дней с привычкой \"" +
                            habit.getTitle() + "\"! Вы настоящий герой!");
        }
    }

    public void checkWeeklyAchievements(Long userId) {
        WeeklySummaryDto weeklySummary = getWeeklySummary(userId);

        if (weeklySummary.taskStats().completionRate() > 80) {
            maxApi.sendText(userId,
                    "📊 Отличная неделя! Вы выполнили " +
                            weeklySummary.taskStats().completionRate() + "% запланированных задач! Так держать!");
        } else if (weeklySummary.taskStats().completionRate() > 50) {
            maxApi.sendText(userId,
                    "📊 Неплохо, но можно ещё улучшить результат! Вы выполнили " +
                            weeklySummary.taskStats().completionRate() + "% запланированных задач!");
        } else {
            maxApi.sendText(userId,
                    "📊 Ой-ой, на этой неделе у вас не лучшая статистика... Вы выполнили всего" +
                            weeklySummary.taskStats().completionRate() +
                            "% запланированных задач. Давайте вместе улучшим этот показатель");
        }

        if (weeklySummary.habitStats().averageCompletionRate() > 80) {
            maxApi.sendText(userId,
                    "💪 Прекрасная работа с привычками! Средний показатель выполнения: " +
                            weeklySummary.habitStats().averageCompletionRate() + "%");
        } else if (weeklySummary.habitStats().averageCompletionRate() > 50) {
            maxApi.sendText(userId,
                    "У вас неплохой результат по работе с привычками! Продолжаем расти! 💪" +
                            "Средний показатель выполнения: " +
                            weeklySummary.habitStats().averageCompletionRate() + "%");
        } else {
            maxApi.sendText(userId,
                    "❗ Стоит улучшить работу с привычками! Средний показатель выполнения: " +
                            weeklySummary.habitStats().averageCompletionRate() + "%");
        }
    }

    @Transactional
    public void showUserStats(long chatId) {
        try {
            WeeklySummaryDto weeklyStats = getWeeklySummary(chatId);
            String statsMessage = formatWeeklyStats(weeklyStats);
            maxApi.sendText(chatId, statsMessage);
        } catch (Exception e) {
            maxApi.sendText(chatId, "Не удалось загрузить статистику. Попробуйте позже.");
        }
    }

    @Transactional
    public void checkAchievements(long chatId) {
        checkAndNotifyAchievements(chatId);
    }

    private String formatWeeklyStats(WeeklySummaryDto stats) {
        return String.format("""
            📊 Ваша статистика за неделю (%s - %s)
            
            📝 Задачи:
            • Всего задач: %d
            • Выполнено: %d (%.1f%%)
            • Просрочено: %d
            
            🔄 Привычки:
            • Активных привычек: %d
            • Средний показатель выполнения: %.1f%%
            • Лучшая серия: %d дней
            
            📈 Самый продуктивный день: %s
            """,
                stats.weekStart(), stats.weekEnd(),
                stats.taskStats().totalTasks(),
                stats.taskStats().completedTasks(),
                stats.taskStats().completionRate(),
                stats.taskStats().overdueTasks(),
                stats.habitStats().activeHabits(),
                stats.habitStats().averageCompletionRate(),
                stats.habitStats().longestStreaks().values().stream().max(Integer::compareTo).orElse(0),
                formatMostActiveDay(stats.activeDays())
        );
    }

    private String formatMostActiveDay(Map<DayOfWeek, Long> activeDays) {
        if (activeDays.isEmpty()) {
            return "Нет данных";
        }

        return activeDays.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> {
                    String dayName = getDayOfWeekName(entry.getKey());
                    return dayName + " (" + entry.getValue() + " задач)";
                })
                .orElse("Нет данных");
    }

    private String getDayOfWeekName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Понедельник";
            case TUESDAY -> "Вторник";
            case WEDNESDAY -> "Среда";
            case THURSDAY -> "Четверг";
            case FRIDAY -> "Пятница";
            case SATURDAY -> "Суббота";
            case SUNDAY -> "Воскресенье";
        };
    }
}