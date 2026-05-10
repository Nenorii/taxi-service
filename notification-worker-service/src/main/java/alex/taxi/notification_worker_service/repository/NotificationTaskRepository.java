package alex.taxi.notification_worker_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import alex.taxi.notification_worker_service.entity.NotificationTask;

import java.util.List;
import java.util.Optional;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {

    List<NotificationTask> findByTripId(Long tripId);

    @Query(value = "SELECT * FROM notification_tasks WHERE status = 'PENDING' " +
            "ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<NotificationTask> claimNextTask();
}
