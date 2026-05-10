package alex.taxi.trip_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripStatsResponse {
    private Long tripsToday;
    private Double averagePrice;
}
