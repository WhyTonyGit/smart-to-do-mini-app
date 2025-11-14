package com.smarttodo.app.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttodo.app.dto.HabitDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
public class PendingHabitRedisRepo {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static final String PREFIX = "bot:pending-habit:";
    private static final Duration TTL = Duration.ofHours(1);

    public PendingHabitRedisRepo(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    private String key(long chatId) {
        return PREFIX + chatId;
    }

    public void save(long chatId, HabitDto dto) {
        String k = key(chatId);
        try {
            String json = objectMapper.writeValueAsString(dto);
            redis.opsForValue().set(k, json, TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize TaskDto to JSON", e);
        }
    }

    public Optional<HabitDto> get(long chatId) {
        String k = key(chatId);
        String json = redis.opsForValue().get(k);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            HabitDto dto = objectMapper.readValue(json, HabitDto.class);
            return Optional.of(dto);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize TaskDto from JSON", e);
            return Optional.empty();
        }
    }

    public void delete(long chatId) {
        redis.delete(key(chatId));
    }
}
