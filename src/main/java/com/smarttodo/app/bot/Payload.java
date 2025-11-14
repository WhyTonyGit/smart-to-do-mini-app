package com.smarttodo.app.bot;

public enum Payload {
    TASK_MENU("tasks-menu"),
    HABIT_MENU("habits-menu"),
    NOTIFICATION_HANDLER("notification-handler"),

    TASKS_CREATE_NEW("tasks-create-new"),
    TASKS_CHANGE_TITLE("tasks-change-title"),
    TASKS_CHANGE_DESCRIPTION("tasks-change-description"),
    TASKS_CHANGE_DEADLINE("tasks-change-deadline"),
    TASKS_CREATE_CONFIRM("tasks-create-confirm"),

    TASKS_GET_TODAY("tasks-get-today"),
    TASKS_GET_WEEK("tasks-get-week"),
    TASKS_GET_ALL("tasks-get-all"),
    TASKS_GET_TOMORROW("tasks-get-tomorrow"),

    HABITS_GET_ALL("habits-get-all"),
    HABITS_GET_TODAY("habits-get-today"),
    HABITS_GET_WEEK("habits-get-week"),
    HABITS_STREAKS("habits-streaks"),
    HABITS_CREATE_NEW("habits-create-new"),
    HABITS_CHANGE_TITLE("habits-change-title"),
    HABITS_CHANGE_DESCRIPTION("habits-change-description"),
    HABITS_CHANGE_INTERVAL("habits-change-interval"),
    HABITS_CHANGE_GOAL_DATE("habits-change-goal-date"),
    HABITS_SET_STATUS_COMPLETED("habits-set-status-completed"),
    HABITS_SET_STATUS_ARCHIVED("habits-set-status-archived"),
    HABITS_SET_STATUS_PAUSED("habits-set-status-paused"),
    HABITS_SET_STATUS_IN_PROGRESS("habits-set-status-in-progress"),
    HABITS_MARK_AS_COMPLETED("habits-mark-as-completed"),
    HABITS_MARK_AS_UNCOMPLETED("habits-mark-as-uncompleted"),

    HOME_PAGE("home-page"),

    TASKS_ID("tasks-id"),
    TASKS_SET_STATUS_UNCOMPLETED("tasks-set-status-uncompleted"),
    TASKS_SET_STATUS_IN_PROGRESS("tasks-set-status-in_progress"),
    TASKS_SET_STATUS_COMPLETED("tasks-set-status-completed"),
    TASKS_DELETE("tasks-delete"),

    HABITS_ID("habits-id");

    private final String key;

    Payload(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Payload from(String raw) {
        if (raw == null) return null;
        String prefix = raw.contains(":") ? raw.split(":")[0] : raw;
        for (Payload p : values()) {
            if (p.key.equals(prefix)) {
                return p;
            }
        }
        return null;
    }

    public boolean hasId() {
        return switch (this) {
            case TASKS_ID,
                 TASKS_SET_STATUS_UNCOMPLETED,
                 TASKS_SET_STATUS_IN_PROGRESS,
                 TASKS_SET_STATUS_COMPLETED,
                 TASKS_DELETE -> true;
            default -> false;
        };
    }

    public Long extractId(String raw) {
        if (!hasId() || raw == null) return null;
        int idx = raw.indexOf(':');
        if (idx < 0 || idx == raw.length() - 1) return null;
        String id = raw.substring(idx + 1);
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean isTasksPayload() {
        return key.startsWith("tasks-");
    }

    public boolean isHabitsPayload() {
        return key.startsWith("habits-");
    }
}

