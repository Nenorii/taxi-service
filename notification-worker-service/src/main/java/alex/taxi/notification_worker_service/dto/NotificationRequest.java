package alex.taxi.notification_worker_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    @NotNull(message = "Trip ID is required")
    private Long tripId;

    @NotBlank(message = "Recipient type is required")
    private String recipientType;

    @NotNull(message = "Recipient ID is required")
    private Long recipientId;

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String message;
}
