package de.evia.travelmate.trips.adapters.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
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

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(RoutingKeys.EXCHANGE);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue tenantCreatedQueue() {
        return new Queue(QUEUE_TENANT_CREATED, true);
    }

    @Bean
    public Queue accountRegisteredQueue() {
        return new Queue(QUEUE_ACCOUNT_REGISTERED, true);
    }

    @Bean
    public Queue dependentAddedQueue() {
        return new Queue(QUEUE_DEPENDENT_ADDED, true);
    }

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
}
