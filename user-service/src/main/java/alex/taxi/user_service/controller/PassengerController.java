package alex.taxi.user_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import alex.taxi.user_service.dto.PassengerRequest;
import alex.taxi.user_service.dto.PassengerResponse;
import alex.taxi.user_service.service.PassengerService;

@RestController
@RequestMapping("/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    @PatchMapping("/{id}")
    public ResponseEntity<PassengerResponse> updateProfile(@PathVariable Long id,
                                                            @Valid @RequestBody PassengerRequest request) {
        return ResponseEntity.ok(passengerService.updatePassengerProfile(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PassengerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(passengerService.getPassenger(id));
    }
}
