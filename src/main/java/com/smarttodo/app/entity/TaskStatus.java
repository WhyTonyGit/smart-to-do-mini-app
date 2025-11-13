package com.smarttodo.app.entity;

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

    public String getEmoji() {
        return emoji;
    }

    public String getDescription() {
        return description;
    }
}
