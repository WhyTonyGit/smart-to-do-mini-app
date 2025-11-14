package com.smarttodo.app.llm.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Результат парсинга текста в список задач")
public record ParseResult(
        @Schema(description = "Список распознанных задач")
        List<ParsedTask> tasks
) {}