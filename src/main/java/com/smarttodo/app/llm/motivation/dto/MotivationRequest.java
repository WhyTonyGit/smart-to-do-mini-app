package com.smarttodo.app.llm.motivation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Запрос на генерацию мотивационного сообщения")
public record MotivationRequest(
        @Schema(
                description = "Количество дней подряд, в течение которых пользователь выполняет свои задачи/привычки",
                example = "7",
                minimum = "1"
        )
        int streakDays
) {}
