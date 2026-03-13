package de.evia.travelmate.iam.adapters.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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

    public static final String QUEUE_EXTERNAL_USER_INVITED = "iam.external-user-invited";
    public static final String QUEUE_EXTERNAL_USER_INVITED_DLQ = QUEUE_EXTERNAL_USER_INVITED + ".dlq";

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(RoutingKeys.EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RoutingKeys.DLX_EXCHANGE);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public Queue externalUserInvitedQueue() {
        return QueueBuilder.durable(QUEUE_EXTERNAL_USER_INVITED)
            .withArgument("x-dead-letter-exchange", RoutingKeys.DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", QUEUE_EXTERNAL_USER_INVITED_DLQ)
            .build();
    }

    @Bean
    public Binding externalUserInvitedBinding(final Queue externalUserInvitedQueue,
                                              final TopicExchange topicExchange) {
        return BindingBuilder.bind(externalUserInvitedQueue).to(topicExchange)
            .with(RoutingKeys.EXTERNAL_USER_INVITED);
    }

    @Bean
    public Queue externalUserInvitedDlq() {
        return QueueBuilder.durable(QUEUE_EXTERNAL_USER_INVITED_DLQ).build();
    }

    @Bean
    public Binding externalUserInvitedDlqBinding(final Queue externalUserInvitedDlq,
                                                  final DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(externalUserInvitedDlq).to(deadLetterExchange)
            .with(QUEUE_EXTERNAL_USER_INVITED_DLQ);
    }
}
