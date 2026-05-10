package alex.taxi.trip_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripStatusRequest {
    @NotBlank(message = "Status is required")
    private String status;
}
