package com.smarttodo.app.repository;

import com.smarttodo.app.bot.MessageMarker;
import com.smarttodo.app.dto.MessageMeta;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
public class LastActionRedisRepo {

    private final StringRedisTemplate redis;
    private static final String PREFIX = "bot:last-message:";
    private static final Duration TTL = Duration.ofDays(30);

    public LastActionRedisRepo(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(long chatId) { return PREFIX + chatId; }

    public boolean save(long chatId, MessageMeta meta) {
        String k = key(chatId);
        BoundHashOperations<String, String, String> h = redis.boundHashOps(k);

        Map<String, String> map = new HashMap<>();
        map.put("mid",    meta.mid());
        map.put("seq",    String.valueOf(meta.seq()));
        map.put("sentAt", String.valueOf(meta.sentAt()));
        map.put("marker", meta.marker() == null ? "" : meta.marker().name());

        h.putAll(map);
        Boolean ok = redis.expire(k, TTL);
        return Boolean.TRUE.equals(ok);
    }

    public Optional<MessageMeta> get(long chatId) {
        String k = key(chatId);
        BoundHashOperations<String, String, String> h = redis.boundHashOps(k);

        Map<String, String> m = h.entries(); // пустая Map, если ключ отсутствует
        if (m == null || m.isEmpty()) return Optional.empty();

        String mid    = m.get("mid");
        long   seq    = parseLong(m.getOrDefault("seq", null), 0L);
        long   sentAt = parseLong(m.getOrDefault("sentAt", null), 0L);
        MessageMarker marker = parseMarker(m.getOrDefault("marker", null));

        return Optional.of(new MessageMeta(mid, seq, sentAt, marker));
    }

    private static MessageMarker parseMarker(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return MessageMarker.valueOf(raw); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private static long parseLong(String s, long def) {
        try { return (s == null) ? def : Long.parseLong(s); }
        catch (NumberFormatException e) { return def; }
    }
}
