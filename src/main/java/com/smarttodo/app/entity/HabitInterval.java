package com.smarttodo.app.entity;

import lombok.Getter;

@Getter
public enum HabitInterval {
    EVERY_DAY("Каждый день"),
    EVERY_WEEK("Каждую неделю"),
    EVERY_WEEKEND("Каждый выходной день"),
    EVERY_WEEKDAY("Каждый будний день"),

    EVERY_SUNDAY("Каждое воскресенье"),
    EVERY_MONDAY("Каждый понедельник"),
    EVERY_TUESDAY("Каждый вторник"),
    EVERY_WEDNESDAY("Каждую среду"),
    EVERY_THURSDAY("Каждый четверг"),
    EVERY_FRIDAY("Каждую пятницу"),
    EVERY_SATURDAY("Каждую субботу");

    private final String displayName;

    HabitInterval(String displayName) {
        this.displayName = displayName;
    }

    public static HabitInterval fromString(String input) {
        if (input == null) {
            return null;
        }
        for (HabitInterval interval : HabitInterval.values()) {
            if (interval.displayName.equalsIgnoreCase(input.trim())) {
                return interval;
            }
        }
        return null;
    }
}
