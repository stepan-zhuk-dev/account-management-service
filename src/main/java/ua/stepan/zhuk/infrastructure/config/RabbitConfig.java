package ua.stepan.zhuk.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    TopicExchange bankingExchange(@Value("${app.rabbit.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    Queue bankingEventsQueue(@Value("${app.rabbit.queue}") String name) {
        return new Queue(name, true);
    }

    @Bean
    Binding bankingBinding(Queue bankingEventsQueue, TopicExchange bankingExchange) {
        return BindingBuilder.bind(bankingEventsQueue).to(bankingExchange).with("#");
    }
}
