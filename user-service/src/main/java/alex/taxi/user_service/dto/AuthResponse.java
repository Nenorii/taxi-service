package alex.taxi.user_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String userType;
    private Long userId;
}
