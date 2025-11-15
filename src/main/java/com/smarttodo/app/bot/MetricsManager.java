package com.smarttodo.app.bot;

import com.smarttodo.app.dto.WeeklySummaryDto;
import com.smarttodo.app.entity.HabitEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MetricsManager {

    private final MessageSender messageSender;

    public void notifyStreakMilestones(Long chatId, HabitEntity habit, int currentStreak) {
        if (currentStreak == 7) {
            messageSender.sendText(chatId,
                    "🎉 Отличный результат! Вы выполняете привычку \"" + habit.getTitle() +
                            "\" уже 7 дней подряд! Так держать!");
        } else if (currentStreak == 30) {
            messageSender.sendText(chatId,
                    "🏆 Потрясающе! 30 дней с привычкой \"" + habit.getTitle() +
                            "\"! Вы формируете устойчивую привычку!");
        } else if (currentStreak % 100 == 0 && currentStreak > 0) {
            messageSender.sendText(chatId,
                    "🌟 Невероятно! Целых " + currentStreak + " дней с привычкой \"" +
                            habit.getTitle() + "\"! Вы настоящий герой!");
        }
    }

    public void notifyWeeklyAchievements(Long chatId, WeeklySummaryDto weeklySummary) {
        if (weeklySummary.taskStats().completionRate() > 80) {
            messageSender.sendText(chatId,
                    "📊 Отличная неделя! Вы выполнили " +
                            String.format("%.1f", weeklySummary.taskStats().completionRate()) +
                            "% запланированных задач! Так держать!");
        } else if (weeklySummary.taskStats().completionRate() > 50) {
            messageSender.sendText(chatId,
                    "📊 Неплохо, но можно ещё улучшить результат! Вы выполнили " +
                            String.format("%.1f", weeklySummary.taskStats().completionRate()) +
                            "% запланированных задач!");
        } else {
            messageSender.sendText(chatId,
                    "📊 Ой-ой, на этой неделе у вас не лучшая статистика... Вы выполнили всего " +
                            String.format("%.1f", weeklySummary.taskStats().completionRate()) +
                            "% запланированных задач. Давайте вместе улучшим этот показатель");
        }

        if (weeklySummary.habitStats().averageCompletionRate() > 80) {
            messageSender.sendText(chatId,
                    "💪 Прекрасная работа с привычками! Средний показатель выполнения: " +
                            String.format("%.1f", weeklySummary.habitStats().averageCompletionRate()) + "%");
        } else if (weeklySummary.habitStats().averageCompletionRate() > 50) {
            messageSender.sendText(chatId,
                    "У вас неплохой результат по работе с привычками! Продолжаем расти! 💪" +
                            "Средний показатель выполнения: " +
                            String.format("%.1f", weeklySummary.habitStats().averageCompletionRate()) + "%");
        } else {
            messageSender.sendText(chatId,
                    "❗ Стоит улучшить работу с привычками! Средний показатель выполнения: " +
                            String.format("%.1f", weeklySummary.habitStats().averageCompletionRate()) + "%");
        }
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

    public void sendWeeklySummary(Long chatId, WeeklySummaryDto weeklySummary) {
        if (!hasWeeklyActivity(weeklySummary)) {
            messageSender.sendText(chatId,
                    """
                            📊 Ваша статистика за неделю
                            
                            На этой неделе у вас не было активных задач или привычек.
                            Начните добавлять задачи и привычки, чтобы видеть свою статистику! 💪""");
            return;
        }

        String weeklyStats = formatWeeklyStats(weeklySummary);
        messageSender.sendText(chatId, weeklyStats);
        notifyWeeklyAchievements(chatId, weeklySummary);
    }

    private boolean hasWeeklyActivity(WeeklySummaryDto weeklySummary) {
        return weeklySummary.taskStats().totalTasks() > 0 ||
                weeklySummary.habitStats().totalHabits() > 0;
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