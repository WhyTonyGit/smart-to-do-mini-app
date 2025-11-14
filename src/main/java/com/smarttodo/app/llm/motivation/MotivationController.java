package com.smarttodo.app.llm.motivation;

import com.smarttodo.app.llm.motivation.dto.MotivationRequest;
import com.smarttodo.app.llm.motivation.dto.MotivationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/motivation")
@RequiredArgsConstructor
@Tag(name = "Motivation", description = "Генерация мотивационных сообщений для пользователей на основе их стрика")
public class MotivationController {
    private final MotivationService motivationService;

    @Operation(
            summary = "Сгенерировать мотивационное сообщение",
            description = """
                    Принимает количество дней подряд, которое пользователь выполняет свои задачи,
                    и возвращает короткое мотивационное сообщение, чтобы поддержать его и помочь не забрасывать привычку.
                    """
    )

    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Мотивационное сообщение успешно сгенерировано",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MotivationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный запрос (например, неположительное количество дней)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Ошибка LLM или внутренняя ошибка сервера"
            )
    })

    @PostMapping(
            value = "/message",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<MotivationResponse> generate(@RequestBody MotivationRequest request) {
        if (request.streakDays() <= 0) return Mono.error(new IllegalArgumentException("streak_days must be > 0"));

        return motivationService.generateMotivation(request.streakDays());
    }
}
