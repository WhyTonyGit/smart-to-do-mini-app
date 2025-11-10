package com.smarttodo.app.dto;

import com.smarttodo.app.entity.Priority;
import com.smarttodo.app.entity.TaskStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public record TaskDto(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        LocalDateTime deadline,
        Instant completedAt
) {
    public TaskDto {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }
    }
}
