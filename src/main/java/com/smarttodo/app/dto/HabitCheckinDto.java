package com.smarttodo.app.dto;

import com.smarttodo.app.entity.HabitInterval;
import com.smarttodo.app.entity.HabitStatus;
import com.smarttodo.app.entity.Priority;

import java.time.LocalDate;

public record HabitCheckinDto(
        Long id,
        String title,
        String description,
        HabitStatus status,
        HabitInterval interval,
        Priority priority,
        LocalDate day,
        boolean completedOnTime
) {
    public HabitCheckinDto {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }
    }
}
