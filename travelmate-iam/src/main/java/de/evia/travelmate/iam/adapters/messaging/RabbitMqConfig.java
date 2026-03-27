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
    public static final String QUEUE_PARTICIPANT_JOINED = "iam.participant-joined";
    public static final String QUEUE_PARTICIPANT_REMOVED = "iam.participant-removed";
    public static final String QUEUE_EXTERNAL_USER_INVITED_DLQ = QUEUE_EXTERNAL_USER_INVITED + ".dlq";
    public static final String QUEUE_PARTICIPANT_JOINED_DLQ = QUEUE_PARTICIPANT_JOINED + ".dlq";
    public static final String QUEUE_PARTICIPANT_REMOVED_DLQ = QUEUE_PARTICIPANT_REMOVED + ".dlq";

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
    public Queue participantJoinedQueue() {
        return QueueBuilder.durable(QUEUE_PARTICIPANT_JOINED)
            .withArgument("x-dead-letter-exchange", RoutingKeys.DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", QUEUE_PARTICIPANT_JOINED_DLQ)
            .build();
    }

    @Bean
    public Queue participantRemovedQueue() {
        return QueueBuilder.durable(QUEUE_PARTICIPANT_REMOVED)
            .withArgument("x-dead-letter-exchange", RoutingKeys.DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", QUEUE_PARTICIPANT_REMOVED_DLQ)
            .build();
    }

    @Bean
    public Binding externalUserInvitedBinding(final Queue externalUserInvitedQueue,
                                              final TopicExchange topicExchange) {
        return BindingBuilder.bind(externalUserInvitedQueue).to(topicExchange)
            .with(RoutingKeys.EXTERNAL_USER_INVITED);
    }

    @Bean
    public Binding participantJoinedBinding(final Queue participantJoinedQueue,
                                            final TopicExchange topicExchange) {
        return BindingBuilder.bind(participantJoinedQueue).to(topicExchange)
            .with(RoutingKeys.PARTICIPANT_CONFIRMED);
    }

    @Bean
    public Binding participantRemovedBinding(final Queue participantRemovedQueue,
                                             final TopicExchange topicExchange) {
        return BindingBuilder.bind(participantRemovedQueue).to(topicExchange)
            .with(RoutingKeys.PARTICIPANT_REMOVED);
    }

    @Bean
    public Queue externalUserInvitedDlq() {
        return QueueBuilder.durable(QUEUE_EXTERNAL_USER_INVITED_DLQ).build();
    }

    @Bean
    public Queue participantJoinedDlq() {
        return QueueBuilder.durable(QUEUE_PARTICIPANT_JOINED_DLQ).build();
    }

    @Bean
    public Queue participantRemovedDlq() {
        return QueueBuilder.durable(QUEUE_PARTICIPANT_REMOVED_DLQ).build();
    }

    @Bean
    public Binding externalUserInvitedDlqBinding(final Queue externalUserInvitedDlq,
                                                  final DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(externalUserInvitedDlq).to(deadLetterExchange)
            .with(QUEUE_EXTERNAL_USER_INVITED_DLQ);
    }

    @Bean
    public Binding participantJoinedDlqBinding(final Queue participantJoinedDlq,
                                               final DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(participantJoinedDlq).to(deadLetterExchange)
            .with(QUEUE_PARTICIPANT_JOINED_DLQ);
    }

    @Bean
    public Binding participantRemovedDlqBinding(final Queue participantRemovedDlq,
                                                final DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(participantRemovedDlq).to(deadLetterExchange)
            .with(QUEUE_PARTICIPANT_REMOVED_DLQ);
    }
}
