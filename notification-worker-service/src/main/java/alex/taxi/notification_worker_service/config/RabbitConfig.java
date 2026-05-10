package alex.taxi.notification_worker_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String TRIP_EXCHANGE = "trip.exchange";
    public static final String TRIP_QUEUE = "trip.events.queue";
    public static final String TRIP_ROUTING_KEY = "trip.status.changed";

    @Bean
    public TopicExchange tripExchange() {
        return new TopicExchange(TRIP_EXCHANGE);
    }

    @Bean
    public Queue tripQueue() {
        return QueueBuilder.durable(TRIP_QUEUE).build();
    }

    @Bean
    public Binding binding(Queue tripQueue, TopicExchange tripExchange) {
        return BindingBuilder.bind(tripQueue).to(tripExchange).with(TRIP_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
