package alex.taxi.user_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import alex.taxi.user_service.dto.DriverResponse;
import alex.taxi.user_service.dto.DriverStatusRequest;
import alex.taxi.user_service.service.DriverService;
import alex.taxi.user_service.service.PassengerService;

import java.util.List;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final PassengerService passengerService;
    private final DriverService driverService;

    @GetMapping("/passengers/{id}/exists")
    public ResponseEntity<Boolean> passengerExists(@PathVariable Long id) {
        return ResponseEntity.ok(passengerService.passengerExists(id));
    }

    @GetMapping("/drivers/available")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers() {
        return ResponseEntity.ok(driverService.getAvailableDrivers());
    }

    @PostMapping("/drivers/assign")
    public ResponseEntity<DriverResponse> assignDriver() {
        return ResponseEntity.ok(driverService.assignAvailableDriver());
    }

    @PatchMapping("/drivers/{id}/status")
    public ResponseEntity<Void> updateDriverStatus(@PathVariable Long id,
                                                    @RequestBody DriverStatusRequest request) {
        driverService.updateDriverStatus(id, request.getStatus());
        return ResponseEntity.ok().build();
    }
}
