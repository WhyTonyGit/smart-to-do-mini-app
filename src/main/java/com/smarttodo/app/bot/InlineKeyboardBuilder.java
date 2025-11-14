package com.smarttodo.app.bot;

import java.util.*;
import java.util.function.Consumer;

public final class InlineKeyboardBuilder {
    private String text;
    private String format;
    private final List<List<Map<String, Object>>> rows = new ArrayList<>();

    private InlineKeyboardBuilder() {}

    public static InlineKeyboardBuilder create() {
        return new InlineKeyboardBuilder();
    }

    public InlineKeyboardBuilder text(String text) {
        this.text = text;
        return this;
    }

    public InlineKeyboardBuilder format(String format) {
        this.format = format;
        return this;
    }

    public InlineKeyboardBuilder addCallbackButton(String buttonText, String payload) {
        rows.add(List.of(callbackButton(buttonText, payload)));
        return this;
    }

    public InlineKeyboardBuilder addMessageButton(String buttonText, String payload) {
        rows.add(List.of(messageButton(buttonText, payload)));
        return this;
    }

    public InlineKeyboardBuilder addRow(Consumer<RowBuilder> rowConfigurer) {
        RowBuilder rb = new RowBuilder();
        rowConfigurer.accept(rb);
        if (!rb.buttons.isEmpty()) rows.add(List.copyOf(rb.buttons));
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text != null ? text : "");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("buttons", List.copyOf(rows));

        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("type", "inline_keyboard");
        attachment.put("payload", payload);

        body.put("attachments", List.of(attachment));
        if (format != null && !format.isBlank()) {
            body.put("format", format);
        }
        return body;
    }


    private static Map<String, Object> callbackButton(String text, String payload) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("type", "callback");
        b.put("text", text);
        b.put("payload", payload);
        return b;
    }

    private static Map<String, Object> messageButton(String text, String payload) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("type", "message");
        b.put("text", text);
        b.put("payload", payload);
        return b;
    }

    public static final class RowBuilder {
        private final List<Map<String, Object>> buttons = new ArrayList<>();

        public RowBuilder callback(String text, String payload) {
            buttons.add(callbackButton(text, payload));
            return this;
        }
    }
}

