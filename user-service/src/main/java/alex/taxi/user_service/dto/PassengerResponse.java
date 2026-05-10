package alex.taxi.user_service.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
}
