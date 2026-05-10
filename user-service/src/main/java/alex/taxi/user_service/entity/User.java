package alex.taxi.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String phone;

    @Column(name = "user_type", nullable = false)
    private String userType;

    @Column(name = "license_number")
    private String licenseNumber;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null && "DRIVER".equals(userType)) {
            status = DriverStatus.AVAILABLE.name();
        }
    }
}
