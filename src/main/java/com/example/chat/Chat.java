package com.example.chat;

import com.example.queue.QueueEvent;
import com.example.queue.QueueException;
import com.example.queue.QueueProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.RabbitFluxException;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class Chat {

    public static final TypeReference<QueueEvent<Message>> QUEUE_EVENT_TYPE_REFERENCE = new TypeReference<>() {
    };
    private static final String QUEUE_EVENT_CONTENT_TYPE = "application/json";

    private final Sender sender;
    private final Receiver receiver;
    private final ObjectMapper objectMapper;
    private final QueueProperties queueProperties;

    public Chat(Sender sender, Receiver receiver, ObjectMapper objectMapper, QueueProperties queueProperties) {
        this.sender = sender;
        this.receiver = receiver;
        this.objectMapper = objectMapper;
        this.queueProperties = queueProperties;
    }

    public Mono<QueueEvent<Message>> send(String text) {
        return Mono.just(text)
            .map(Message::withContent)
            .map(QueueEvent::createEvent)
            .delayUntil(event ->
                Mono.from(serializeEvent(event))
                    .flatMapMany(messageBody -> sender.sendWithTypedPublishConfirms(Flux.just(messageBody)))
                    .onErrorMap(RabbitFluxException.class, QueueException::new)
            );
    }

    public Flux<Message> read() {
        return receiver.consumeManualAck(queueProperties.getQueue())
            .doOnNext(AcknowledgableDelivery::ack)
            .flatMap(delivery -> deserializeEvent(delivery.getBody()))
            .map(QueueEvent::getPayload);
    }

    private <T> Mono<OutboundMessage> serializeEvent(QueueEvent<T> event) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(event))
            .onErrorMap(JsonMappingException.class, QueueException::new)
            .map(eventSerialized ->
                new OutboundMessage(
                    queueProperties.getExchange(),
                    queueProperties.getRoutingKey(),
                    new AMQP.BasicProperties.Builder()
                        .messageId(UUID.randomUUID().toString())
                        .appId(queueProperties.getAppId())
                        .deliveryMode(queueProperties.getDeliveryMode())
                        .contentType(QUEUE_EVENT_CONTENT_TYPE)
                        .contentEncoding(StandardCharsets.UTF_8.displayName())
                        .build(),
                    eventSerialized
                )
            )
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<QueueEvent<Message>> deserializeEvent(byte[] event) {
        return Mono.fromCallable(() -> objectMapper.readValue(event, QUEUE_EVENT_TYPE_REFERENCE))
            .onErrorMap(JsonParseException.class, QueueException::new)
            .subscribeOn(Schedulers.boundedElastic());
    }
}
