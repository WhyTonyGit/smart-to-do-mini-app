package com.smarttodo.app.entity;

import lombok.Getter;

@Getter
public enum TaskStatus {
    UNCOMPLETED("❌", "Выполнение не начато"),
    IN_PROGRESS("🔄", "В процессе"),
    COMPLETED("✅", "Выполнено");

    private final String emoji;
    private final String description;

    TaskStatus(String emoji, String description) {
        this.emoji = emoji;
        this.description = description;
    }
}
