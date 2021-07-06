package com.example.testing;

import com.example.AppConfiguration;
import com.example.queue.QueueConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(SpringExtension.class)
@SpringJUnitConfig({QueueConfiguration.class, AppConfiguration.class})
@Testcontainers
public abstract class IntegrationTestConfiguration {

    private static final String IMAGE = "rabbitmq:3.7.25-management-alpine";

    @Container
    private static final RabbitMQContainer CONTAINER = new RabbitMQContainer(DockerImageName.parse(IMAGE));

    @DynamicPropertySource
    private static void queueProperties(DynamicPropertyRegistry registry) {
        registry.add("rabbitmq.username", CONTAINER::getAdminUsername);
        registry.add("rabbitmq.password", CONTAINER::getAdminPassword);
        registry.add("rabbitmq.host", CONTAINER::getHost);
        registry.add("rabbitmq.port", CONTAINER::getAmqpPort);
    }
}
