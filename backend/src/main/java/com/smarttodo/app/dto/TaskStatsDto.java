package com.smarttodo.app.dto;

import com.smarttodo.app.entity.Priority;

import java.time.LocalDate;
import java.util.Map;

public record TaskStatsDto(
        long totalTasks,
        long completedTasks,
        long overdueTasks,
        Map<Priority, Long> tasksByPriority,
        double averageCompletionTimeSeconds,
        LocalDate periodStart,
        LocalDate periodEnd
) {
    public double completionRate() {
        return totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0.0;
    }
}