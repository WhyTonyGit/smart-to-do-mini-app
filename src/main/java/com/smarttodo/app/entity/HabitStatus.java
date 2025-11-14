package com.smarttodo.app.entity;

import lombok.Getter;

@Getter
public enum HabitStatus {
    ARCHIVED("📦"),
    IN_PROGRESS("🔄"),
    PAUSED("⏸️"),
    COMPLETED("✅");

    private final String emoji;

    HabitStatus(String emoji) {
        this.emoji = emoji;
    }
}