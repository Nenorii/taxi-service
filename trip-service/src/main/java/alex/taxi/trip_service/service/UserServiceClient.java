package alex.taxi.trip_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import alex.taxi.trip_service.dto.DriverResponse;

import java.util.List;
import java.util.Map;

@Service
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Value("${user-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public boolean passengerExists(Long id) {
        return Boolean.TRUE.equals(
                restClient.get()
                        .uri("/internal/passengers/{id}/exists", id)
                        .retrieve()
                        .body(Boolean.class));
    }

    public List<DriverResponse> getAvailableDrivers() {
        return restClient.get()
                .uri("/internal/drivers/available")
                .retrieve()
                .body(new ParameterizedTypeReference<List<DriverResponse>>() {});
    }

    public DriverResponse assignDriver() {
        return restClient.post()
                .uri("/internal/drivers/assign")
                .retrieve()
                .body(DriverResponse.class);
    }

    public void updateDriverStatus(Long driverId, String status) {
        restClient.patch()
                .uri("/internal/drivers/{id}/status", driverId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", status))
                .retrieve()
                .toBodilessEntity();
    }
}
