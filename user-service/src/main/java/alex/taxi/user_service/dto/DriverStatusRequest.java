package alex.taxi.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverStatusRequest {
    @NotBlank(message = "Status is required")
    private String status;
}
