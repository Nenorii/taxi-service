package alex.taxi.user_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import alex.taxi.user_service.dto.*;
import alex.taxi.user_service.entity.User;
import alex.taxi.user_service.exception.BadRequestException;
import alex.taxi.user_service.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        String userType = request.getUserType().toUpperCase();
        if (!"PASSENGER".equals(userType) && !"DRIVER".equals(userType)) {
            throw new BadRequestException("Invalid user type: " + userType);
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .userType(userType)
                .licenseNumber(request.getLicenseNumber())
                .build();
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), userType, user.getId());
        return AuthResponse.builder()
                .token(token)
                .userType(userType)
                .userId(user.getId())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getUserType(), user.getId());
        return AuthResponse.builder()
                .token(token)
                .userType(user.getUserType())
                .userId(user.getId())
                .build();
    }
}
