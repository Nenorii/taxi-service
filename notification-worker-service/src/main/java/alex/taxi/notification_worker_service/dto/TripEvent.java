package alex.taxi.notification_worker_service.dto;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripEvent implements Serializable {
    private Long tripId;
    private Long passengerId;
    private Long driverId;
    private String status;
    private String origin;
    private String destination;
    private Double price;
}
