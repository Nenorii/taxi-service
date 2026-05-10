package alex.taxi.trip_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import alex.taxi.trip_service.config.RabbitConfig;
import alex.taxi.trip_service.dto.TripEvent;

@Service
@RequiredArgsConstructor
public class TripEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishTripEvent(TripEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.TRIP_EXCHANGE,
                RabbitConfig.TRIP_ROUTING_KEY,
                event);
    }
}
