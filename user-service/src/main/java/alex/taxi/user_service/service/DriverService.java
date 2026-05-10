package alex.taxi.user_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import alex.taxi.user_service.dto.DriverRequest;
import alex.taxi.user_service.dto.DriverResponse;
import alex.taxi.user_service.entity.DriverStatus;
import alex.taxi.user_service.entity.User;
import alex.taxi.user_service.exception.BadRequestException;
import alex.taxi.user_service.exception.ResourceNotFoundException;
import alex.taxi.user_service.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final UserRepository userRepository;

    @Transactional
    public DriverResponse assignAvailableDriver() {
        User driver = userRepository.findFirstDriverByStatus(DriverStatus.AVAILABLE.name())
                .orElseThrow(() -> new ResourceNotFoundException("No available drivers"));
        driver.setStatus(DriverStatus.BUSY.name());
        driver = userRepository.save(driver);
        return toResponse(driver);
    }

    @Cacheable(value = "availableDrivers")
    public List<DriverResponse> getAvailableDrivers() {
        return userRepository.findDriversByStatus(DriverStatus.AVAILABLE.name())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DriverResponse getDriver(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + id));
        if (!"DRIVER".equals(user.getUserType())) {
            throw new ResourceNotFoundException("Driver not found: " + id);
        }
        return toResponse(user);
    }

    @Transactional
    public DriverResponse updateDriverProfile(Long id, DriverRequest request) {
        User driver = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + id));
        if (!"DRIVER".equals(driver.getUserType())) {
            throw new ResourceNotFoundException("Driver not found: " + id);
        }
        driver.setName(request.getName());
        driver.setPhone(request.getPhone());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver = userRepository.save(driver);
        return toResponse(driver);
    }

    @CacheEvict(value = "availableDrivers", allEntries = true)
    public DriverResponse updateDriverStatus(Long id, String status) {
        User driver = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + id));
        if (!"DRIVER".equals(driver.getUserType())) {
            throw new ResourceNotFoundException("Driver not found: " + id);
        }
        driver.setStatus(status);
        driver = userRepository.save(driver);
        return toResponse(driver);
    }

    private DriverResponse toResponse(User u) {
        return DriverResponse.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .licenseNumber(u.getLicenseNumber())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
