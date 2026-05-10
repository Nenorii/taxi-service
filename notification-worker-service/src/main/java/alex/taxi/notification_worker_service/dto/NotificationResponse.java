package alex.taxi.notification_worker_service.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private Long tripId;
    private String recipientType;
    private Long recipientId;
    private String message;
    private String status;
    private Integer attempts;
    private LocalDateTime createdAt;
}
