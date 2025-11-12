package com.smarttodo.app.entity;


public enum HabitStatus {
    /**
     * Привычка добавлена в планах, однако пользователь
     * еще не начал ее выполнение, стоит по умолчанию
     */
    ARCHIVED,

    /**
     * Все понятно, устанавливается при первом выполнении привычки
     */
    IN_PROGRESS,

    PAUSED,

    COMPLETED
}
