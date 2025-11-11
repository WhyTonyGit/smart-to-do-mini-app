package com.smarttodo.app.llm;

import com.smarttodo.app.llm.dto.ParseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class NlpService {

    private final OllamaClient ollama;

    public Mono<ParseResult> parseText(String text) {
        var zone = ZoneId.of("Europe/Moscow");
        var today = LocalDate.now(zone).format(DateTimeFormatter.ISO_DATE);
        var now   = LocalTime.now(zone).withSecond(0).withNano(0).format(DateTimeFormatter.ofPattern("HH:mm"));

        String system = """
Ты — детерминированный ПАРСЕР задач. Вход — короткая фраза на русском. 
Выход — СТРОГО валидный JSON по схеме ниже. Никакого текста, только JSON.

СХЕМА:
{
  "tasks": [
    {
      "title": string,             // 1..80 символов, действие в инфинитиве/краткой форме: "позвонить маме", "поиграть в компьютер"
      "description": string|null,  // детали, если явно есть; иначе null
      "date": string|null,         // YYYY-MM-DD или null
      "time": string|null,         // HH:mm (24ч) или null
      "priority": "low"|"normal"|"high"|null,
      "splitOf": string|null       // всегда null (мы не разбиваем тут)
    }
  ]
}

ЖЕСТКИЕ ПРАВИЛА:
- Задача = явное намерение СДЕЛАТЬ действие. Ничего не выдумывать.
- Если действие задано — не переименовывать на другой смысл. "поиграть" не превращать в "поставить задачу".
- Если дата не указана, ставь "date": null. НЕЛЬЗЯ подставлять today={TODAY} по умолчанию.
- Время нормализуй в 24-часовой формат:
  * "в 12 дня", "в полдень"  => "12:00"
  * "в 12 ночи", "в полночь" => "00:00"
- Если время не указано — "time": null.
- Форматы ДОЛЖНЫ быть ровно такими: date "YYYY-MM-DD", time "HH:mm". Никаких ISO-datetime, "or null", диапазонов, AM/PM и т.п.
- Приоритет выставляй ТОЛЬКО если он явно упомянут:
  * "срочно", "очень важно", "немедленно" → "high"
  * "важно", "приоритетно" → "normal"
  * "по возможности", "когда будет время" → "low"
  Иначе "priority": null.
- description — только если в тексте есть уточнения ("для проекта X", "с Петей") — иначе null.
- Никаких дополнительных задач и дубликатов.

ПРИМЕРЫ (образцы поведения):
ВХОД: "Поиграть в компьютер в 12 дня"
ВЫХОД:
{
  "tasks": [
    {
      "title": "поиграть в компьютер",
      "description": null,
      "date": null,
      "time": "12:00",
      "priority": null,
      "splitOf": null
    }
  ]
}

ВХОД: "Позвонить маме завтра в 8 утра"
(считать завтра относительно today={TODAY})
ВЫХОД:
{
  "tasks": [
    {
      "title": "позвонить маме",
      "description": null,
      "date": "%TOMORROW%",
      "time": "08:00",
      "priority": null,
      "splitOf": null
    }
  ]
}

ВХОД: "Срочно отправить отчёт по проекту"
ВЫХОД:
{
  "tasks": [
    {
      "title": "отправить отчёт",
      "description": "по проекту",
      "date": null,
      "time": null,
      "priority": "high",
      "splitOf": null
    }
  ]
}

Ответ — СТРОГО в JSON по схеме.
""".replace("{TODAY}", today);

        String user = """
                Исходный текст:
                \"\"\"
                %s
                \"\"\"

                Контекст:
                - today=%s
                - now=%s

                Сформируй JSON строго по заданной схеме и правилам.
                """.formatted(text, today, now);

        return ollama.chatExtractJson(system, user);
    }
}
