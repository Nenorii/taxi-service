package alex.taxi.notification_worker_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import alex.taxi.notification_worker_service.dto.NotificationRequest;
import alex.taxi.notification_worker_service.dto.NotificationResponse;
import alex.taxi.notification_worker_service.entity.NotificationTask;
import alex.taxi.notification_worker_service.repository.NotificationTaskRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationTaskRepository taskRepository;

    public NotificationResponse createTask(NotificationRequest request) {
        NotificationTask task = NotificationTask.builder()
                .tripId(request.getTripId())
                .recipientType(request.getRecipientType())
                .recipientId(request.getRecipientId())
                .message(request.getMessage())
                .build();
        task = taskRepository.save(task);
        return toResponse(task);
    }

    public List<NotificationResponse> getByTripId(Long tripId) {
        return taskRepository.findByTripId(tripId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponse toResponse(NotificationTask t) {
        return NotificationResponse.builder()
                .id(t.getId())
                .tripId(t.getTripId())
                .recipientType(t.getRecipientType())
                .recipientId(t.getRecipientId())
                .message(t.getMessage())
                .status(t.getStatus().name())
                .attempts(t.getAttempts())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
