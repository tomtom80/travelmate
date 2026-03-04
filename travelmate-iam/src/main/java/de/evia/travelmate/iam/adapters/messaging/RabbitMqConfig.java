package de.evia.travelmate.iam.adapters.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import de.evia.travelmate.common.messaging.RoutingKeys;

@Configuration
@Profile("!test")
public class RabbitMqConfig {

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(RoutingKeys.EXCHANGE);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
