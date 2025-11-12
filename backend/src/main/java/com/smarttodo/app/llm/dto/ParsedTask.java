package com.smarttodo.app.llm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Одна распаршенная задача")
public record ParsedTask(
        @Schema(description = "Заголовок задачи: действие в инфинитиве или краткой форме", example = "позвонить маме", maxLength = 80)
        String title,

        @Schema(description = "Детали задачи, если указаны явно", example = "по проекту X", nullable = true)
        String description,

        @Schema(description = "Дата в формате YYYY-MM-DD", example = "2025-11-12", nullable = true, pattern = "\\d{4}-\\d{2}-\\d{2}")
        String date,

        @Schema(description = "Время в 24-часовом формате HH:mm", example = "08:00", nullable = true, pattern = "\\d{2}:\\d{2}")
        String time,

        @Schema(description = "Приоритет задачи", example = "high", nullable = true,
                allowableValues = {"low", "normal", "high"})
        String priority,

        @Schema(description = "Идентификатор родительской задачи при разбивке (всегда null)", example = "null", nullable = true)
        String splitOf
) {}