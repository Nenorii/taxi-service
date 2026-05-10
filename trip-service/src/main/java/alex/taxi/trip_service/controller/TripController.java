package alex.taxi.trip_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import alex.taxi.trip_service.dto.*;
import alex.taxi.trip_service.service.TripService;

import java.util.List;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public ResponseEntity<TripResponse> create(@Valid @RequestBody TripRequest request) {
        return ResponseEntity.status(201).body(tripService.createTrip(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTrip(id));
    }

    @GetMapping
    public ResponseEntity<List<TripResponse>> getByPassenger(@RequestParam("passenger_id") Long passengerId) {
        return ResponseEntity.ok(tripService.getTripsByPassenger(passengerId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TripResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody TripStatusRequest request) {
        return ResponseEntity.ok(tripService.updateTripStatus(id, request));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<TripResponse> rate(@PathVariable Long id,
                                              @Valid @RequestBody RatingRequest request) {
        return ResponseEntity.ok(tripService.rateTrip(id, request));
    }

    @GetMapping("/stats")
    public ResponseEntity<TripStatsResponse> stats() {
        return ResponseEntity.ok(tripService.getStats());
    }
}
