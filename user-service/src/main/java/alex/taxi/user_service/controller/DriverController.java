package alex.taxi.user_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import alex.taxi.user_service.dto.DriverRequest;
import alex.taxi.user_service.dto.DriverResponse;
import alex.taxi.user_service.dto.DriverStatusRequest;
import alex.taxi.user_service.service.DriverService;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PatchMapping("/{id}")
    public ResponseEntity<DriverResponse> updateProfile(@PathVariable Long id,
                                                         @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(driverService.updateDriverProfile(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.getDriver(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DriverResponse> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody DriverStatusRequest request) {
        return ResponseEntity.ok(driverService.updateDriverStatus(id, request.getStatus()));
    }
}
