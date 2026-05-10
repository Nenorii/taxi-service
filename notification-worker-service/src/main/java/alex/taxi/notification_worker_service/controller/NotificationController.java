package alex.taxi.notification_worker_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import alex.taxi.notification_worker_service.dto.NotificationRequest;
import alex.taxi.notification_worker_service.dto.NotificationResponse;
import alex.taxi.notification_worker_service.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(201).body(notificationService.createTask(request));
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getByTripId(@RequestParam("trip_id") Long tripId) {
        return ResponseEntity.ok(notificationService.getByTripId(tripId));
    }
}
