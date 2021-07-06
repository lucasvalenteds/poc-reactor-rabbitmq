package com.example.chat;

import com.example.queue.QueueException;
import com.example.testing.UnitTestConfiguration;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.RabbitFluxException;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChatTest extends UnitTestConfiguration {

    private final Chat chat = new Chat(sender, receiver, objectMapper, queueProperties);

    @Test
    void testHandlingBrokerError() throws JsonProcessingException {
        Mockito.when(objectMapper.writeValueAsBytes(Mockito.any()))
            .thenAnswer(invocation -> objectMapperImplementation.writeValueAsBytes(invocation.getArgument(0)));

        Mockito.when(sender.sendWithTypedPublishConfirms(Mockito.any()))
            .thenReturn(Flux.error(new RabbitFluxException()));

        StepVerifier.create(chat.send("text"))
            .expectErrorSatisfies(throwable -> {
                assertEquals(QueueException.class, throwable.getClass());
                assertEquals("Could not publish message to the broker", throwable.getMessage());
                assertNotNull(throwable.getCause());
                assertEquals(RabbitFluxException.class, throwable.getCause().getClass());
            })
            .verify();

        Mockito.verify(sender, Mockito.times(1))
            .sendWithTypedPublishConfirms(Mockito.any());
    }

    @Test
    void testHandlingSerializationError() throws JsonProcessingException {
        Mockito.when(objectMapper.writeValueAsBytes(Mockito.any()))
            .thenThrow(JsonMappingException.fromUnexpectedIOE(new IOException()));

        StepVerifier.create(chat.send("text"))
            .expectErrorSatisfies(throwable -> {
                assertEquals(QueueException.class, throwable.getClass());
                assertEquals("Could not serialize the event payload", throwable.getMessage());
                assertNotNull(throwable.getCause());
                assertEquals(JsonMappingException.class, throwable.getCause().getClass());
            })
            .verify();

        Mockito.verify(objectMapper, Mockito.times(1))
            .writeValueAsBytes(Mockito.any());
    }

    @Test
    void testHandlingDeserializationError() throws IOException {
        Mockito.when(objectMapper.readValue(Mockito.any(byte[].class), Mockito.eq(Chat.QUEUE_EVENT_TYPE_REFERENCE)))
            .thenThrow(new JsonParseException(null, "message"));

        var ackDelivery = Mockito.mock(AcknowledgableDelivery.class);
        Mockito.when(ackDelivery.getBody())
            .thenReturn("not-valid-JSON-here".getBytes(StandardCharsets.UTF_8));

        Mockito.when(receiver.consumeManualAck(Mockito.eq(queueProperties.getQueue())))
            .thenReturn(Flux.just(ackDelivery));

        StepVerifier.create(chat.read())
            .expectErrorSatisfies(throwable -> {
                assertEquals(QueueException.class, throwable.getClass());
                assertEquals("Could not deserialize the event payload", throwable.getMessage());
                assertNotNull(throwable.getCause());
                assertEquals(JsonParseException.class, throwable.getCause().getClass());
            })
            .verify();

        Mockito.verify(receiver, Mockito.times(1))
            .consumeManualAck(Mockito.eq(queueProperties.getQueue()));
    }
}
