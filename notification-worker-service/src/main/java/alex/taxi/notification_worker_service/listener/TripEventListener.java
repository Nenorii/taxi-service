package alex.taxi.notification_worker_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import alex.taxi.notification_worker_service.dto.NotificationRequest;
import alex.taxi.notification_worker_service.dto.TripEvent;
import alex.taxi.notification_worker_service.service.NotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "trip.events.queue")
    public void handleTripEvent(TripEvent event) {
        log.info("Received trip event: tripId={}, status={}", event.getTripId(), event.getStatus());

        notificationService.createTask(NotificationRequest.builder()
                .tripId(event.getTripId())
                .recipientType("PASSENGER")
                .recipientId(event.getPassengerId())
                .message(buildPassengerMessage(event))
                .build());

        if (event.getDriverId() != null) {
            notificationService.createTask(NotificationRequest.builder()
                    .tripId(event.getTripId())
                    .recipientType("DRIVER")
                    .recipientId(event.getDriverId())
                    .message(buildDriverMessage(event))
                    .build());
        }
    }

    private String buildPassengerMessage(TripEvent event) {
        return switch (event.getStatus()) {
            case "DRIVER_ASSIGNED" -> String.format(
                    "Driver assigned to your trip #%d from %s to %s. Price: %.2f",
                    event.getTripId(), event.getOrigin(), event.getDestination(), event.getPrice());
            case "IN_PROGRESS" -> String.format("Your trip #%d has started!", event.getTripId());
            case "COMPLETED" -> String.format(
                    "Trip #%d completed. Total: %.2f. Please rate your ride!",
                    event.getTripId(), event.getPrice());
            case "CANCELLED" -> String.format("Trip #%d has been cancelled.", event.getTripId());
            default -> String.format("Trip #%d status: %s", event.getTripId(), event.getStatus());
        };
    }

    private String buildDriverMessage(TripEvent event) {
        return switch (event.getStatus()) {
            case "DRIVER_ASSIGNED" -> String.format(
                    "New trip #%d assigned: %s -> %s",
                    event.getTripId(), event.getOrigin(), event.getDestination());
            case "COMPLETED" -> String.format("Trip #%d completed. Good job!", event.getTripId());
            case "CANCELLED" -> String.format("Trip #%d cancelled. You are now available.", event.getTripId());
            default -> String.format("Trip #%d status: %s", event.getTripId(), event.getStatus());
        };
    }
}
