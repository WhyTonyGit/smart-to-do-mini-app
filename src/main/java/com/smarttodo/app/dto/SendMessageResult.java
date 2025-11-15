package com.smarttodo.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageResult {
    @JsonProperty("message")
    public MessageDto message;

    public String getMid() {
        if (message != null && message.getBody() != null) {
            return message.getBody().getMid();
        }
        return null;
    }

    public Long getSeq() {
        if (message != null && message.getBody() != null) {
            return message.getBody().getSeq();
        }
        return null;
    }

    public Long getTs() {
        if (message != null) {
            return message.getTs();
        }
        return null;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageDto {
        @JsonProperty("body")
        public Body body;

        @JsonProperty("timestamp")
        public Long ts;

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Body {
            @JsonProperty("mid")
            public String mid;
            @JsonProperty("seq")
            public Long seq;
        }

    }
}
