package com.example.queue;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import reactor.rabbitmq.RabbitFluxException;

public final class QueueException extends RuntimeException {

    private QueueException(String operation, Throwable throwable) {
        super("Could not " + operation + " the event payload", throwable);
    }

    public QueueException(JsonMappingException exception) {
        this("serialize", exception);
    }

    public QueueException(JsonParseException exception) {
        this("deserialize", exception);
    }

    public QueueException(RabbitFluxException exception) {
        super("Could not publish message to the broker", exception);
    }
}
