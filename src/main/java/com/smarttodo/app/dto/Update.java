package com.smarttodo.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

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

    public boolean isType(String expected) {
        return expected != null && expected.equalsIgnoreCase(updateType);
    }

    public boolean isTextCommand(String cmd) {
        return isType("message_created")
                && message != null
                && message.getBody() != null
                && message.getBody().getText().trim().equalsIgnoreCase(cmd);
    }

    public boolean isCallback(String code) {
        return isType("message_callback")
                && callback != null
                && code != null
                && code.equalsIgnoreCase(callback.getPayload());
    }

    public long chatId() {
        return message != null && message.getRecipient() != null
                ? message.getRecipient().getChatId()
                : 0L;
    }

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private MessageBody body;
        private Recipient recipient;
        private Long timestamp;
    }

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Recipient {
        private long chatId;

    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageBody {
        private String text; // вот где лежит текст сообщения
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Callback {
        private String payload;     // то, что ты положил в кнопке
        private Recipient recipient; // откуда нажали
    }
}
