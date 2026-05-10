package alex.taxi.user_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import alex.taxi.user_service.dto.PassengerRequest;
import alex.taxi.user_service.dto.PassengerResponse;
import alex.taxi.user_service.entity.User;
import alex.taxi.user_service.exception.ResourceNotFoundException;
import alex.taxi.user_service.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class PassengerService {

    private final UserRepository userRepository;

    public PassengerResponse getPassenger(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found: " + id));
        if (!"PASSENGER".equals(user.getUserType())) {
            throw new ResourceNotFoundException("Passenger not found: " + id);
        }
        return toResponse(user);
    }

    public boolean passengerExists(Long id) {
        return userRepository.existsByIdAndUserType(id, "PASSENGER");
    }

    @Transactional
    public PassengerResponse updatePassengerProfile(Long id, PassengerRequest request) {
        User passenger = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found: " + id));
        if (!"PASSENGER".equals(passenger.getUserType())) {
            throw new ResourceNotFoundException("Passenger not found: " + id);
        }
        passenger.setName(request.getName());
        passenger.setPhone(request.getPhone());
        passenger = userRepository.save(passenger);
        return toResponse(passenger);
    }

    private PassengerResponse toResponse(User u) {
        return PassengerResponse.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
