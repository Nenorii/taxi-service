package alex.taxi.trip_service.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import alex.taxi.trip_service.entity.Trip;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByPassengerId(Long passengerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trip t WHERE t.id = :id")
    Optional<Trip> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.createdAt >= :startOfDay")
    Long countTripsToday(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT AVG(t.price) FROM Trip t WHERE t.price IS NOT NULL")
    Double averagePrice();
}
