package alex.taxi.notification_worker_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import alex.taxi.notification_worker_service.entity.NotificationTask;
import alex.taxi.notification_worker_service.entity.TaskStatus;
import alex.taxi.notification_worker_service.repository.NotificationTaskRepository;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWorker {

    private static final int MAX_ATTEMPTS = 3;
    private final NotificationTaskRepository taskRepository;

    @Transactional
    public boolean processNextTask() {
        Optional<NotificationTask> optTask = taskRepository.claimNextTask();
        if (optTask.isEmpty()) {
            return false;
        }

        NotificationTask task = optTask.get();
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setAttempts(task.getAttempts() + 1);
        taskRepository.save(task);

        try {
            sendNotification(task);
            task.setStatus(TaskStatus.SENT);
            taskRepository.save(task);
            log.info("[Worker-{}] Notification {} sent: {} -> {} ({})",
                    Thread.currentThread().getName(), task.getId(),
                    task.getRecipientType(), task.getRecipientId(), task.getMessage());
            return true;
        } catch (Exception e) {
            log.error("[Worker-{}] Failed to send notification {}: {}",
                    Thread.currentThread().getName(), task.getId(), e.getMessage());
            if (task.getAttempts() >= MAX_ATTEMPTS) {
                task.setStatus(TaskStatus.FAILED);
                log.warn("[Worker-{}] Notification {} permanently failed after {} attempts",
                        Thread.currentThread().getName(), task.getId(), MAX_ATTEMPTS);
            } else {
                task.setStatus(TaskStatus.PENDING);
            }
            taskRepository.save(task);
            return true;
        }
    }

    private void sendNotification(NotificationTask task) throws InterruptedException {
        Thread.sleep(500 + (long) (Math.random() * 1000));
        log.info("[NOTIFICATION] To {} {}: {}", task.getRecipientType(),
                task.getRecipientId(), task.getMessage());
    }
}
