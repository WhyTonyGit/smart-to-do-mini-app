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
      "splitOf": string|null       // всегда null
    }
  ]
}

ЖЁСТКИЕ ПРАВИЛА:
- Задача = явное намерение СДЕЛАТЬ действие. Ничего не выдумывать.
- Не менять смысл действия. "поиграть" не превращать в "создать задачу".
- ДАТА:
  * "сегодня" → date = "{TODAY}"
  * "завтра"  → date = "%TOMORROW%"
  * иначе — date = null. Слово "срочно"/"немедленно" НЕ означает дату.
- ВРЕМЯ:
  * Явное время вида "в HH:mm" → time = "HH:mm"
  * Спец-слова: "в полдень" → "12:00"; "в полночь"/"в 12 ночи" → "00:00"
  * Иначе — time = null. Слова "сейчас", "как можно скорее", "срочно" — НЕ время.
- ПРИОРИТЕТ:
  * "срочно", "очень важно", "немедленно" → "high"
  * "важно", "приоритетно" → "normal"
  * "по возможности", "когда будет время" → "low"
  * иначе — null
- description — только если в тексте есть уточнения ("по проекту X", "с Петей"); иначе null.
- Никаких дополнительных задач и дубликатов.
- Строго соблюдай форматы: date "YYYY-MM-DD", time "HH:mm". Никаких ISO-datetime/AM-PM/диапазонов.

АНТИ-ПРИМЕРЫ (чтобы НЕ ошибаться):
ВХОД: "срочно погулять с собакой"
ПРАВИЛЬНО:
{
  "tasks": [
    {
      "title": "погулять с собакой",
      "description": null,
      "date": null,
      "time": null,
      "priority": "high",
      "splitOf": null
    }
  ]
}

ВХОД: "Позвонить маме завтра в 8 утра"
(завтра относительно today={TODAY})
ПРАВИЛЬНО:
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

ВХОД: "Поиграть в компьютер в 12 дня"
ПРАВИЛЬНО:
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

Ответ — СТРОГО в JSON по схеме.
""".replace("{TODAY}", today);

        String user = """
Исходный текст:
\"\"\"
%s
\"\"\"

Контекст:
- today=%s
- now=%s  // ВНИМАНИЕ: "now" не означает время для задачи. Используется только в примерах.
Сформируй JSON строго по заданной схеме и правилам.
""".formatted(text, today, now);

        return ollama.chatExtractJson(system, user);
    }
}
