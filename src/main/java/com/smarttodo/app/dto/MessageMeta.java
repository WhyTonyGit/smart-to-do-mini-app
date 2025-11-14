package com.smarttodo.app.dto;

import com.smarttodo.app.bot.MessageMarker;

public record MessageMeta(
        String mid,
        long seq,
        long sentAt,
        MessageMarker marker
) {
    @Override
    public String toString() {
        return "MessageMeta {\n" +
                "  mid = '" + mid + "',\n" +
                "  seq = " + seq + ",\n" +
                "  sentAt = " + sentAt + ",\n" +
                "  marker = " + marker + "\n" +
                "}";
    }
}