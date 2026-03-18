package de.evia.travelmate.expense.adapters.messaging;

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

    public static final String QUEUE_TRIP_CREATED = "expense.trip-created";
    public static final String QUEUE_PARTICIPANT_JOINED = "expense.participant-joined";
    public static final String QUEUE_TRIP_COMPLETED = "expense.trip-completed";
    public static final String QUEUE_STAY_PERIOD_UPDATED = "expense.stay-period-updated";
    public static final String QUEUE_ACCOMMODATION_PRICE_SET = "expense.accommodation-price-set";

    public static final String QUEUE_TRIP_CREATED_DLQ = QUEUE_TRIP_CREATED + ".dlq";
    public static final String QUEUE_PARTICIPANT_JOINED_DLQ = QUEUE_PARTICIPANT_JOINED + ".dlq";
    public static final String QUEUE_TRIP_COMPLETED_DLQ = QUEUE_TRIP_COMPLETED + ".dlq";
    public static final String QUEUE_STAY_PERIOD_UPDATED_DLQ = QUEUE_STAY_PERIOD_UPDATED + ".dlq";
    public static final String QUEUE_ACCOMMODATION_PRICE_SET_DLQ = QUEUE_ACCOMMODATION_PRICE_SET + ".dlq";

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

    // --- Main queues with DLX arguments ---

    @Bean
    public Queue tripCreatedQueue() {
        return QueueBuilder.durable(QUEUE_TRIP_CREATED)
            .withArgument("x-dead-letter-exchange", RoutingKeys.DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", QUEUE_TRIP_CREATED_DLQ)
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
    public Queue tripCompletedQueue() {
        return QueueBuilder.durable(QUEUE_TRIP_COMPLETED)
            .withArgument("x-dead-letter-exchange", RoutingKeys.DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", QUEUE_TRIP_COMPLETED_DLQ)
            .build();
    }

    @Bean
    public Queue stayPeriodUpdatedQueue() {
        return QueueBuilder.durable(QUEUE_STAY_PERIOD_UPDATED)
            .withArgument("x-dead-letter-exchange", RoutingKeys.DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", QUEUE_STAY_PERIOD_UPDATED_DLQ)
            .build();
    }

    // --- Main queue bindings ---

    @Bean
    public Binding tripCreatedBinding(final Queue tripCreatedQueue, final TopicExchange topicExchange) {
        return BindingBuilder.bind(tripCreatedQueue).to(topicExchange).with(RoutingKeys.TRIP_CREATED);
    }

    @Bean
    public Binding participantJoinedBinding(final Queue participantJoinedQueue, final TopicExchange topicExchange) {
        return BindingBuilder.bind(participantJoinedQueue).to(topicExchange).with(RoutingKeys.PARTICIPANT_CONFIRMED);
    }

    @Bean
    public Binding tripCompletedBinding(final Queue tripCompletedQueue, final TopicExchange topicExchange) {
        return BindingBuilder.bind(tripCompletedQueue).to(topicExchange).with(RoutingKeys.TRIP_COMPLETED);
    }

    @Bean
    public Binding stayPeriodUpdatedBinding(final Queue stayPeriodUpdatedQueue,
                                             final TopicExchange topicExchange) {
        return BindingBuilder.bind(stayPeriodUpdatedQueue).to(topicExchange)
            .with(RoutingKeys.STAY_PERIOD_UPDATED);
    }

    @Bean
    public Queue accommodationPriceSetQueue() {
        return QueueBuilder.durable(QUEUE_ACCOMMODATION_PRICE_SET)
            .withArgument("x-dead-letter-exchange", RoutingKeys.DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", QUEUE_ACCOMMODATION_PRICE_SET_DLQ)
            .build();
    }

    // --- Main queue bindings ---

    @Bean
    public Binding accommodationPriceSetBinding(final Queue accommodationPriceSetQueue,
                                                 final TopicExchange topicExchange) {
        return BindingBuilder.bind(accommodationPriceSetQueue).to(topicExchange)
            .with(RoutingKeys.ACCOMMODATION_PRICE_SET);
    }

    // --- Dead letter queues ---

    @Bean
    public Queue tripCreatedDlq() {
        return QueueBuilder.durable(QUEUE_TRIP_CREATED_DLQ).build();
    }

    @Bean
    public Queue participantJoinedDlq() {
        return QueueBuilder.durable(QUEUE_PARTICIPANT_JOINED_DLQ).build();
    }

    @Bean
    public Queue tripCompletedDlq() {
        return QueueBuilder.durable(QUEUE_TRIP_COMPLETED_DLQ).build();
    }

    @Bean
    public Queue stayPeriodUpdatedDlq() {
        return QueueBuilder.durable(QUEUE_STAY_PERIOD_UPDATED_DLQ).build();
    }

    // --- Dead letter queue bindings ---

    @Bean
    public Binding tripCreatedDlqBinding(final Queue tripCreatedDlq, final DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(tripCreatedDlq).to(deadLetterExchange).with(QUEUE_TRIP_CREATED_DLQ);
    }

    @Bean
    public Binding participantJoinedDlqBinding(final Queue participantJoinedDlq,
                                                final DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(participantJoinedDlq).to(deadLetterExchange).with(QUEUE_PARTICIPANT_JOINED_DLQ);
    }

    @Bean
    public Binding tripCompletedDlqBinding(final Queue tripCompletedDlq,
                                            final DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(tripCompletedDlq).to(deadLetterExchange).with(QUEUE_TRIP_COMPLETED_DLQ);
    }

    @Bean
    public Binding stayPeriodUpdatedDlqBinding(final Queue stayPeriodUpdatedDlq,
                                                final DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(stayPeriodUpdatedDlq).to(deadLetterExchange)
            .with(QUEUE_STAY_PERIOD_UPDATED_DLQ);
    }

    @Bean
    public Queue accommodationPriceSetDlq() {
        return QueueBuilder.durable(QUEUE_ACCOMMODATION_PRICE_SET_DLQ).build();
    }

    @Bean
    public Binding accommodationPriceSetDlqBinding(final Queue accommodationPriceSetDlq,
                                                    final DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(accommodationPriceSetDlq).to(deadLetterExchange)
            .with(QUEUE_ACCOMMODATION_PRICE_SET_DLQ);
    }
}
