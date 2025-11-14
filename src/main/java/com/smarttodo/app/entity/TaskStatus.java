package com.smarttodo.app.entity;

import lombok.Getter;

@Getter
public enum TaskStatus {
    NEW("❌", "Выполнение не начато"),
    IN_PROGRESS("⏳", "В процессе"),
    DONE("✅", "Выполнено");

    private final String emoji;
    private final String description;

    TaskStatus(String emoji, String description) {
        this.emoji = emoji;
        this.description = description;
    }
}
