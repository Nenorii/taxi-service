package alex.taxi.trip_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PriceCalculator {

    private final double ratePerKm;

    private static final Map<String, double[]> KNOWN_LOCATIONS = new ConcurrentHashMap<>();

    static {
        KNOWN_LOCATIONS.put("moscow, lenina 1",   new double[]{55.7558, 37.6173});
        KNOWN_LOCATIONS.put("moscow, pushkina 10", new double[]{55.7658, 37.6073});
        KNOWN_LOCATIONS.put("moscow, kreml",       new double[]{55.7512, 37.6185});
        KNOWN_LOCATIONS.put("moscow, tverskaya 1", new double[]{55.7580, 37.6130});
        KNOWN_LOCATIONS.put("spb, nevsky 1",       new double[]{59.9343, 30.3351});
        KNOWN_LOCATIONS.put("kazan, baumana 1",    new double[]{55.7879, 49.1233});
        KNOWN_LOCATIONS.put("novosibirsk, lenina 1", new double[]{55.0302, 82.9204});
        KNOWN_LOCATIONS.put("ekaterinburg, lenina 1", new double[]{56.8389, 60.6057});
    }

    public PriceCalculator(@Value("${price.rate-per-km}") double ratePerKm) {
        this.ratePerKm = ratePerKm;
    }

    public double calculate(String origin, String destination) {
        double[] from = resolveCoordinates(origin);
        double[] to = resolveCoordinates(destination);
        double distanceKm = haversine(from[0], from[1], to[0], to[1]);
        return Math.round(distanceKm * ratePerKm * 100.0) / 100.0;
    }

    private double[] resolveCoordinates(String address) {
        return KNOWN_LOCATIONS.getOrDefault(
                address.toLowerCase().strip(),
                new double[]{55.7558, 37.6173} // Москва по умолчанию
        );
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
