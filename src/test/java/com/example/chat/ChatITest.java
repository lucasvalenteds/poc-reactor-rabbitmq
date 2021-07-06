package com.example.chat;

import com.example.queue.QueueEvent;
import com.example.queue.QueueProperties;
import com.example.testing.IntegrationTestConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChatITest extends IntegrationTestConfiguration {

    private static final String MESSAGE_1 = "Have a great day, Mary!";
    private static final String MESSAGE_2 = "Thanks John. Have a great day too!";

    private Chat chat;

    @BeforeEach
    void beforeEach(ApplicationContext context) {
        this.chat = new Chat(
            context.getBean(Sender.class),
            context.getBean(Receiver.class),
            context.getBean(ObjectMapper.class),
            context.getBean(QueueProperties.class)
        );
    }

    @RepeatedTest(100)
    void testSendingAndReadingMessages() {
        var sending = Flux.just(MESSAGE_1, MESSAGE_2)
            .flatMap(chat::send);

        StepVerifier.create(sending)
            .expectNextCount(1L)
            .assertNext(event -> {
                assertNotNull(event.getId());
                assertEquals(UUID.class, event.getId().getClass());

                assertEquals(QueueEvent.Version.V1.name(), event.getVersion());

                assertNotNull(event.getTimestamp());

                assertNotNull(event.getPayload());
                assertNotNull(event.getPayload().getId());
                assertEquals(UUID.class, event.getPayload().getId().getClass());
                assertNotNull(event.getPayload().getContent());
            })
            .verifyComplete();

        var reading = chat.read()
            .take(2)
            .collectList();

        StepVerifier.create(reading)
            .assertNext(messages ->
                assertThat(messages)
                    .hasSize(2)
                    .extracting(Message::getContent)
                    .containsOnly(MESSAGE_1, MESSAGE_2)
            )
            .verifyComplete();
    }
}
