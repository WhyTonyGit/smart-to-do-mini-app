package com.smarttodo.app.llm.task;

import com.smarttodo.app.llm.OllamaClient;
import com.smarttodo.app.llm.task.dto.ParseResult;
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

            ПРАВИЛА ПАРСИНГА ДАТЫ И ВРЕМЕНИ:
            
            Сегодня: {TODAY}
            Завтра: {TOMORROW}
            
            1. ДАТА:
               - Если в тексте есть слово "сегодня" → используй дату {TODAY}
               - Если в тексте есть слово "завтра" → используй дату {TOMORROW}
               - Если даты нет → datetime = null
            
            2. ВРЕМЯ:
               - "в 19:00", "в 19 часов", "в 7 вечера" → "19:00"
               - "в полдень" → "12:00"
               - "в полночь", "в 12 ночи" → "00:00"
               - "утром" → "09:00"
               - "днём" → "14:00"
               - "вечером" → "19:00"
               - "ночью" → "23:00"
            
            3. ФОРМАТ datetime:
               - Если есть И дата И время: "dd.MM.yyyy HH:mm" (пример: "14.11.2025 19:00")
               - Если есть ТОЛЬКО дата: "dd.MM.yyyy" (пример: "14.11.2025")
               - Если НЕТ даты: null
            
            4. ПРИОРИТЕТ:
               - "срочно"/"немедленно"/"очень важно" → "high"
               - "важно"/"приоритетно" → "normal"
               - "по возможности"/"когда будет время" → "low"
               - иначе → null
            
            5. TITLE:
               - Извлеки основное действие БЕЗ указаний времени
               - "Завтра в 19:00 выпить сок" → title: "Выпить сок"
            
            ВАЖНО:
            - "сейчас"/"как можно скорее" НЕ являются датой/временем
            - Не выдумывай информацию
            - Ответ ТОЛЬКО валидный JSON, без комментариев
            
            ПРИМЕРЫ:
            Вход: "Завтра в 19:00 выпить сок"
            Выход: {{"tasks": [{{"title": "Выпить сок", "description": null, "datetime": "{TOMORROW} 19:00", "priority": null, "splitOf": null}}]}}
            
            Вход: "Сегодня купить молоко"
            Выход: {{"tasks": [{{"title": "Купить молоко", "description": null, "datetime": "{TODAY}", "priority": null, "splitOf": null}}]}}
            """
                .replace("{TODAY}", today)
                .replace("{TOMORROW}", tomorrow);

        String user = text;

        return ollama.chatExtractJson(system, user);
    }
}