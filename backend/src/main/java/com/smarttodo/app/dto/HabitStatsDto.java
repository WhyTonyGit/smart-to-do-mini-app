package com.smarttodo.app.dto;

import java.time.LocalDate;
import java.util.Map;

public record HabitStatsDto(
        int totalHabits,
        int activeHabits,
        Map<Long, Double> completionRates, // habitId -> completion rate
        Map<Long, Integer> longestStreaks,
        Map<Long, Integer> currentStreaks,
        LocalDate periodStart,
        LocalDate periodEnd
) {
    public double averageCompletionRate() {
        return completionRates.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
}