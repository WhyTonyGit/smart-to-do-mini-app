
package com.smarttodo.app.llm;

import com.smarttodo.app.llm.dto.ParseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class NlpService {

    private final OllamaClient ollama;

    public Mono<ParseResult> parseText(String text) {
        var zone = ZoneId.of("Europe/Moscow");
        var todayDate = LocalDate.now(zone);

        var today = todayDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        var tomorrow = todayDate.plusDays(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        String system = """
            Ты — детерминированный ПАРСЕР задач.
            Вход: короткая фраза на русском.
            Выход: СТРОГО валидный JSON по схеме:

            {
              "tasks": [
                {
                  "title": string,
                  "description": string|null,
                  "datetime": string|null,
                  "priority": "low"|"normal"|"high"|null,
                  "splitOf": string|null
                }
              ]
            }

            ПРАВИЛА:
            - Не выдумывать действия.
            - "сегодня" → datetime = "{TODAY} HH:mm" (если есть время) или "{TODAY}" (если времени нет)
            - "завтра"  → datetime = "{TOMORROW} HH:mm" (если есть время) или "{TOMORROW}" (если времени нет)
            - иначе datetime = null
            - "в HH:mm" → добавить время к дате в формате "dd.MM.yyyy HH:mm"
            - "в полдень" → "12:00"; "в полночь"/"в 12 ночи" → "00:00"
            - если время не указано, datetime содержит только дату "dd.MM.yyyy"
            - "срочно"/"немедленно"/"очень важно" → priority="high"
            - "важно"/"приоритетно" → "normal"
            - "по возможности"/"когда будет время" → "low"
            - "сейчас"/"как можно скорее" НЕ являются датой/временем.
            
            ФОРМАТ datetime:
            - С временем: "dd.MM.yyyy HH:mm"
            - Без времени: "dd.MM.yyyy"
            - Если нет даты: null
            
            Ответ — строго JSON.
            """
                .replace("{TODAY}", today)
                .replace("{TOMORROW}", tomorrow);

        String user = text;

        return ollama.chatExtractJson(system, user);
    }
}