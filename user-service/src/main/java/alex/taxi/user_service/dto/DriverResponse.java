package alex.taxi.user_service.dto;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverResponse implements Serializable {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String licenseNumber;
    private String status;
    private LocalDateTime createdAt;
}
