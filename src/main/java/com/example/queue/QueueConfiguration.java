package com.example.queue;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.BindingSpecification;
import reactor.rabbitmq.ExchangeSpecification;
import reactor.rabbitmq.QueueSpecification;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;

@Configuration
@PropertySource("classpath:application.properties")
public class QueueConfiguration {

    @Autowired
    private Environment environment;

    @Autowired
    private Mono<Connection> connection;

    @Autowired
    private Sender sender;

    @Bean
    QueueProperties queueProperties() {
        return new QueueProperties(
            environment.getRequiredProperty("rabbitmq.app.id", String.class),
            environment.getRequiredProperty("rabbitmq.app.deliveryMode", Integer.class),
            environment.getRequiredProperty("rabbitmq.messages.queue.name", String.class),
            environment.getRequiredProperty("rabbitmq.messages.exchange.name", String.class),
            environment.getRequiredProperty("rabbitmq.messages.binding.routingKey", String.class)
        );
    }

    @Bean
    ConnectionFactory connectionFactory() {
        var factory = new ConnectionFactory();

        factory.setUsername(environment.getRequiredProperty("rabbitmq.username", String.class));
        factory.setPassword(environment.getRequiredProperty("rabbitmq.password", String.class));
        factory.setHost(environment.getRequiredProperty("rabbitmq.host", String.class));
        factory.setPort(environment.getRequiredProperty("rabbitmq.port", Integer.class));

        return factory;
    }

    @Bean
    Mono<Connection> monoConnection(ConnectionFactory factory) {
        return Mono.fromCallable(factory::newConnection)
            .cache();
    }

    @Bean
    Sender sender(Mono<Connection> connection) {
        return RabbitFlux.createSender(
            new SenderOptions()
                .connectionMono(connection)
        );
    }

    @Bean
    Receiver receiver(Mono<Connection> connection) {
        return RabbitFlux.createReceiver(
            new ReceiverOptions()
                .connectionMono(connection)
        );
    }

    @PostConstruct
    void construct() {
        var queue = sender.declareQueue(
            QueueSpecification.queue()
                .name(environment.getRequiredProperty("rabbitmq.messages.queue.name", String.class))
                .durable(environment.getRequiredProperty("rabbitmq.messages.queue.durable", Boolean.class))
                .exclusive(environment.getRequiredProperty("rabbitmq.messages.queue.exclusive", Boolean.class))
                .autoDelete(environment.getRequiredProperty("rabbitmq.messages.queue.delete", Boolean.class))
        );

        var exchange = sender.declareExchange(
            ExchangeSpecification.exchange()
                .name(environment.getRequiredProperty("rabbitmq.messages.exchange.name", String.class))
                .durable(environment.getRequiredProperty("rabbitmq.messages.exchange.durable", Boolean.class))
                .autoDelete(environment.getRequiredProperty("rabbitmq.messages.exchange.delete", Boolean.class))
        );

        var binding = sender.bind(
            new BindingSpecification()
                .exchange(environment.getRequiredProperty("rabbitmq.messages.exchange.name", String.class))
                .routingKey(environment.getRequiredProperty("rabbitmq.messages.binding.routingKey", String.class))
                .queue(environment.getRequiredProperty("rabbitmq.messages.queue.name", String.class))
                .arguments(Map.of())
        );

        Mono.from(queue)
            .then(exchange)
            .then(binding)
            .delaySubscription(
                Mono.from(connection)
                    .filter(ShutdownNotifier::isOpen)
                    .hasElement()
            )
            .subscribe();
    }

    @PreDestroy
    void destroy() throws IOException {
        var instance = this.connection.blockOptional();
        if (instance.isPresent()) {
            instance.get().close();
        }
    }
}
