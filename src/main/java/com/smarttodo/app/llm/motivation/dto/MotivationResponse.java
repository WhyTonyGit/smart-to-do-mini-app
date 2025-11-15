package com.smarttodo.app.llm.motivation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ с мотивационным сообщением")
public record MotivationResponse(
        @Schema(
                description = "Короткое мотивационное сообщение для пользователя",
                example = "Ты уже 7 дней подряд держишься, это очень сильный результат. Продолжай в том же духе — маленькие шаги каждый день приводят к большим изменениям."
        )
        String message
) {}
