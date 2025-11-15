package com.smarttodo.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Update {
    private String eventId;
    // тип события, например "message_created"
    private String updateType;
    private Long timestamp;
    private Message message;
    private Callback callback;

    private static final String NL = System.lineSeparator();

    private static String fmtTs(Long ts) {
        if (ts == null) return "null";
        try {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    .format(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()))
                    + " (" + ts + ")";
        } catch (Exception e) {
            return String.valueOf(ts);
        }
    }

    private static String indent(String s, int spaces) {
        if (s == null) return "null";
        String pad = " ".repeat(spaces);
        return Arrays.stream(s.split("\\R", -1))
                .map(line -> pad + line)
                .collect(Collectors.joining(NL));
    }

    public boolean isType(String expected) {
        return expected != null && expected.equalsIgnoreCase(updateType);
    }

    public boolean isTextCommand(String cmd) {
        return isType("message_created")
                && message != null
                && message.getBody() != null
                && message.getBody().getText().trim().equalsIgnoreCase(cmd);
    }

    public boolean isText() {
        return isType("message_created")
                && message != null
                && message.getBody() != null;
    }

    public String getText() {
        if (message != null &&  message.getBody() != null) {
            return message.getBody().getText().trim();
        }
        return null;
    }

    public boolean isCallback(String code) {
        return isType("message_callback")
                && callback != null
                && code != null
                && code.equalsIgnoreCase(callback.getPayload());
    }

    public boolean isCallback() {
        return isType("message_callback")
                && callback != null;
    }

    public String getPayload() {
        if (callback == null) {
            return null;
        }
        return callback.getPayload();
    }

    public long chatId() {
        return message != null && message.getRecipient() != null
                ? message.getRecipient().getChatId()
                : 0L;
    }

    public long userId() {
        // message_created: message.sender.user_id
        if (message != null && message.getSender() != null && message.getSender().getUserId() != null) {
            return message.getSender().getUserId();
        }
        // message_callback: callback.user.user_id
        if (callback != null && callback.getUser() != null && callback.getUser().getUserId() != null) {
            return callback.getUser().getUserId();
        }
        return 0L;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Update").append(NL);
        sb.append("  eventId: ").append(eventId).append(NL);
        sb.append("  updateType: ").append(updateType).append(NL);
        sb.append("  timestamp: ").append(fmtTs(timestamp)).append(NL);
        sb.append("  message:").append(NL)
                .append(message == null ? "    null" : indent(message.toString(), 4)).append(NL);
        sb.append("  callback:").append(NL)
                .append(callback == null ? "    null" : indent(callback.toString(), 4));
        return sb.toString();
    }


    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private MessageBody body;
        private Recipient recipient;
        private Long timestamp;
        private Sender sender;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Message").append(NL);
            sb.append("  timestamp: ").append(fmtTs(timestamp)).append(NL);
            sb.append("  recipient:").append(NL)
                    .append(recipient == null ? "    null" : indent(recipient.toString(), 4)).append(NL);
            sb.append("  body:").append(NL)
                    .append(body == null ? "    null" : indent(body.toString(), 4));
            return sb.toString();
        }
    }

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sender {

        @JsonProperty("user_id")
        private Long userId;
    }

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Recipient {
        private long chatId;

        @Override
        public String toString() {
            return "Recipient" + NL +
                    "  chatId: " + chatId;
        }

    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageBody {
        private String text; // вот где лежит текст сообщения

        @Override
        public String toString() {
            return "MessageBody" + NL +
                    "  text: " + (text == null ? "null" : text);
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Callback {
        private String payload;
        private Recipient recipient;
        private Sender user;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Callback").append(NL);
            sb.append("  payload: ").append(payload).append(NL);
            sb.append("  recipient:").append(NL)
                    .append(recipient == null ? "    null" : indent(recipient.toString(), 4));
            return sb.toString();
        }
    }
}
