package de.evia.travelmate.iam.adapters.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
        final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
        return new Jackson2JsonMessageConverter(mapper);
    }
}
