package de.evia.travelmate.trips.adapters.messaging;

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

    public static final String QUEUE_TENANT_CREATED = "trips.tenant-created";
    public static final String QUEUE_ACCOUNT_REGISTERED = "trips.account-registered";
    public static final String QUEUE_DEPENDENT_ADDED = "trips.dependent-added";

    public static final String QUEUE_TENANT_CREATED_DLQ = QUEUE_TENANT_CREATED + ".dlq";
    public static final String QUEUE_ACCOUNT_REGISTERED_DLQ = QUEUE_ACCOUNT_REGISTERED + ".dlq";
    public static final String QUEUE_DEPENDENT_ADDED_DLQ = QUEUE_DEPENDENT_ADDED + ".dlq";

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
    public Queue tenantCreatedQueue() {
        return QueueBuilder.durable(QUEUE_TENANT_CREATED)
            .withArgument("x-dead-letter-exchange", RoutingKeys.DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", QUEUE_TENANT_CREATED_DLQ)
            .build();
    }

    @Bean
    public Queue accountRegisteredQueue() {
        return QueueBuilder.durable(QUEUE_ACCOUNT_REGISTERED)
            .withArgument("x-dead-letter-exchange", RoutingKeys.DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", QUEUE_ACCOUNT_REGISTERED_DLQ)
            .build();
    }

    @Bean
    public Queue dependentAddedQueue() {
        return QueueBuilder.durable(QUEUE_DEPENDENT_ADDED)
            .withArgument("x-dead-letter-exchange", RoutingKeys.DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", QUEUE_DEPENDENT_ADDED_DLQ)
            .build();
    }

    // --- Main queue bindings ---

    @Bean
    public Binding tenantCreatedBinding(final Queue tenantCreatedQueue, final TopicExchange topicExchange) {
        return BindingBuilder.bind(tenantCreatedQueue).to(topicExchange).with(RoutingKeys.TENANT_CREATED);
    }

    @Bean
    public Binding accountRegisteredBinding(final Queue accountRegisteredQueue, final TopicExchange topicExchange) {
        return BindingBuilder.bind(accountRegisteredQueue).to(topicExchange).with(RoutingKeys.ACCOUNT_REGISTERED);
    }

    @Bean
    public Binding dependentAddedBinding(final Queue dependentAddedQueue, final TopicExchange topicExchange) {
        return BindingBuilder.bind(dependentAddedQueue).to(topicExchange).with(RoutingKeys.DEPENDENT_ADDED);
    }

    // --- Dead letter queues ---

    @Bean
    public Queue tenantCreatedDlq() {
        return QueueBuilder.durable(QUEUE_TENANT_CREATED_DLQ).build();
    }

    @Bean
    public Queue accountRegisteredDlq() {
        return QueueBuilder.durable(QUEUE_ACCOUNT_REGISTERED_DLQ).build();
    }

    @Bean
    public Queue dependentAddedDlq() {
        return QueueBuilder.durable(QUEUE_DEPENDENT_ADDED_DLQ).build();
    }

    // --- Dead letter queue bindings ---

    @Bean
    public Binding tenantCreatedDlqBinding(final Queue tenantCreatedDlq, final DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(tenantCreatedDlq).to(deadLetterExchange).with(QUEUE_TENANT_CREATED_DLQ);
    }

    @Bean
    public Binding accountRegisteredDlqBinding(final Queue accountRegisteredDlq,
                                                final DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(accountRegisteredDlq).to(deadLetterExchange).with(QUEUE_ACCOUNT_REGISTERED_DLQ);
    }

    @Bean
    public Binding dependentAddedDlqBinding(final Queue dependentAddedDlq,
                                             final DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(dependentAddedDlq).to(deadLetterExchange).with(QUEUE_DEPENDENT_ADDED_DLQ);
    }
}
