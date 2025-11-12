package com.smarttodo.app.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

public record WeeklySummaryDto(
        HabitStatsDto habitStats,
        TaskStatsDto taskStats,
        Map<DayOfWeek, Long> activeDays,
        LocalDate weekStart,
        LocalDate weekEnd
) {}