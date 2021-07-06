package com.example.testing;

import com.example.AppConfiguration;
import com.example.queue.QueueProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;

@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(AppConfiguration.class)
public abstract class UnitTestConfiguration {

    protected final Sender sender = Mockito.mock(Sender.class);
    protected final Receiver receiver = Mockito.mock(Receiver.class);
    protected final ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
    protected final QueueProperties queueProperties =
        new QueueProperties("appId", 0, "queue", "exchange", "routingKey");

    protected ObjectMapper objectMapperImplementation;

    @BeforeEach
    void beforeEach(ApplicationContext context) {
        this.objectMapperImplementation = context.getBean(ObjectMapper.class);
    }
}
