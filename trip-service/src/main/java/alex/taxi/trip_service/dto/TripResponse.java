package alex.taxi.trip_service.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripResponse {
    private Long id;
    private Long passengerId;
    private Long driverId;
    private String status;
    private String origin;
    private String destination;
    private Double price;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
