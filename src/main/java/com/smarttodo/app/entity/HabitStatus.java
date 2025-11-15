package com.smarttodo.app.entity;

import lombok.Getter;

@Getter
public enum HabitStatus {
    ARCHIVED("📦", "не взята в работу"),
    IN_PROGRESS("🔄", "в процессе"),
    PAUSED("⏸️", "на паузе"),
    COMPLETED("✅", "завершена");

    private final String emoji;
    private final String description;

    HabitStatus(String emoji, String description) {
        this.emoji = emoji;
        this.description = description;
    }
}