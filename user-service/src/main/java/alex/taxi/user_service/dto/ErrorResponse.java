package alex.taxi.user_service.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private String message;
    private int status;
    private LocalDateTime timestamp;
}
