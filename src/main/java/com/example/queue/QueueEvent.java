package com.example.queue;

import java.time.LocalDateTime;
import java.util.UUID;

public final class QueueEvent<PAYLOAD> {

    public enum Version {
        V1
    }

    private UUID id;
    private String version;
    private LocalDateTime timestamp;
    private PAYLOAD payload;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public PAYLOAD getPayload() {
        return payload;
    }

    public void setPayload(PAYLOAD payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "QueueEvent{" +
            "id=" + id +
            ", version='" + version + '\'' +
            ", timestamp=" + timestamp +
            ", payload=" + payload +
            '}';
    }

    public static <T> QueueEvent<T> createEvent(T payload) {
        var event = new QueueEvent<T>();

        event.setId(UUID.randomUUID());
        event.setTimestamp(LocalDateTime.now());
        event.setVersion(Version.V1.name());
        event.setPayload(payload);

        return event;
    }
}
