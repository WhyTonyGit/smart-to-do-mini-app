package com.smarttodo.app.dto;

public record MeResponse(
        long user_id,
        String name,
        String username,
        boolean is_bot,
        long last_activity_time
) {}
