package alex.taxi.trip_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import alex.taxi.trip_service.dto.*;
import alex.taxi.trip_service.entity.Trip;
import alex.taxi.trip_service.entity.TripStatus;
import alex.taxi.trip_service.exception.BadRequestException;
import alex.taxi.trip_service.exception.ResourceNotFoundException;
import alex.taxi.trip_service.repository.TripRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final UserServiceClient userServiceClient;
    private final TripEventPublisher tripEventPublisher;
    private final PriceCalculator priceCalculator;

    @Transactional
    public TripResponse createTrip(TripRequest request) {
        if (!userServiceClient.passengerExists(request.getPassengerId())) {
            throw new BadRequestException("Passenger not found: " + request.getPassengerId());
        }

        DriverResponse driver = userServiceClient.assignDriver();
        double price = calculatePrice(request.getOrigin(), request.getDestination());

        Trip trip = Trip.builder()
                .passengerId(request.getPassengerId())
                .driverId(driver.getId())
                .status(TripStatus.DRIVER_ASSIGNED)
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .price(price)
                .build();
        trip = tripRepository.save(trip);

        log.info("Trip {} created, driver {} assigned", trip.getId(), driver.getId());
        publishEvent(trip);
        return toResponse(trip);
    }

    public TripResponse getTrip(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + id));
        return toResponse(trip);
    }

    public List<TripResponse> getTripsByPassenger(Long passengerId) {
        return tripRepository.findByPassengerId(passengerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TripResponse updateTripStatus(Long id, TripStatusRequest request) {
        Trip trip = tripRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + id));

        TripStatus newStatus = TripStatus.valueOf(request.getStatus());
        validateStatusTransition(trip.getStatus(), newStatus);
        trip.setStatus(newStatus);

        if (newStatus == TripStatus.COMPLETED || newStatus == TripStatus.CANCELLED) {
            if (trip.getDriverId() != null) {
                userServiceClient.updateDriverStatus(trip.getDriverId(), "AVAILABLE");
            }
        }

        trip = tripRepository.save(trip);
        log.info("Trip {} status updated to {}", id, newStatus);
        publishEvent(trip);
        return toResponse(trip);
    }

    @Transactional
    public TripResponse rateTrip(Long id, RatingRequest request) {
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + id));
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new BadRequestException("Can only rate completed trips");
        }
        trip.setRating(request.getRating());
        trip = tripRepository.save(trip);
        return toResponse(trip);
    }

    public TripStatsResponse getStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Long tripsToday = tripRepository.countTripsToday(startOfDay);
        Double averagePrice = tripRepository.averagePrice();
        return TripStatsResponse.builder()
                .tripsToday(tripsToday != null ? tripsToday : 0L)
                .averagePrice(averagePrice != null ? averagePrice : 0.0)
                .build();
    }

    public double calculatePrice(String origin, String destination) {
        return priceCalculator.calculate(origin, destination);
    }

    private void validateStatusTransition(TripStatus current, TripStatus next) {
        boolean valid = switch (current) {
            case CREATED -> next == TripStatus.DRIVER_ASSIGNED || next == TripStatus.CANCELLED;
            case DRIVER_ASSIGNED -> next == TripStatus.IN_PROGRESS || next == TripStatus.CANCELLED;
            case IN_PROGRESS -> next == TripStatus.COMPLETED || next == TripStatus.CANCELLED;
            default -> false;
        };
        if (!valid) {
            throw new BadRequestException("Invalid status transition: " + current + " -> " + next);
        }
    }

    private void publishEvent(Trip trip) {
        TripEvent event = TripEvent.builder()
                .tripId(trip.getId())
                .passengerId(trip.getPassengerId())
                .driverId(trip.getDriverId())
                .status(trip.getStatus().name())
                .origin(trip.getOrigin())
                .destination(trip.getDestination())
                .price(trip.getPrice())
                .build();
        tripEventPublisher.publishTripEvent(event);
    }

    private TripResponse toResponse(Trip t) {
        return TripResponse.builder()
                .id(t.getId())
                .passengerId(t.getPassengerId())
                .driverId(t.getDriverId())
                .status(t.getStatus().name())
                .origin(t.getOrigin())
                .destination(t.getDestination())
                .price(t.getPrice())
                .rating(t.getRating())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
