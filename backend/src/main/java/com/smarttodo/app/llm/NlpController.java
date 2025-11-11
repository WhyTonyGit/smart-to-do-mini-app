package com.smarttodo.app.llm;

import com.smarttodo.app.llm.dto.ParseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/nlp")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "NLP Parser", description = "Парсинг естественного языка в структурированные задачи")
public class NlpController {

    private final NlpService nlp;

    @Operation(
            summary = "Распарсить текст на русском в список задач",
            description = """
                    Принимает текст на русском языке и возвращает структурированный JSON с задачами.
                    Поддерживает даты (относительно today), время, приоритеты и уточнения.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешный парсинг",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ParseResult.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный запрос (пустой текст и т.п.)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Ошибка LLM или парсинга JSON"
            )
    })
    @PostMapping(value = "/parse", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ParseResult> parse(
            @RequestBody(
                    description = "Тело запроса с текстом для парсинга",
                    required = true,
                    content = @Content(schema = @Schema(example = """
                            {
                              "text": "Позвонить маме завтра в 8 утра срочно"
                            }
                            """))
            )
            @org.springframework.web.bind.annotation.RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "");
        return nlp.parseText(text);
    }
}