package com.example.chat;

import java.util.UUID;

public final class Message {

    private UUID id;
    private String content;

    Message() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public static Message withContent(String content) {
        var message = new Message();

        message.setId(UUID.randomUUID());
        message.setContent(content);

        return message;
    }
}
