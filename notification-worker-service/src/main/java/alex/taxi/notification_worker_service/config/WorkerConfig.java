package alex.taxi.notification_worker_service.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import alex.taxi.notification_worker_service.service.NotificationWorker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class WorkerConfig {

    private final NotificationWorker notificationWorker;
    private final int poolSize;
    private final long pollInterval;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public WorkerConfig(NotificationWorker notificationWorker,
                        @Value("${worker.pool-size:4}") int poolSize,
                        @Value("${worker.poll-interval:2000}") long pollInterval) {
        this.notificationWorker = notificationWorker;
        this.poolSize = poolSize;
        this.pollInterval = pollInterval;
        this.executor = Executors.newFixedThreadPool(poolSize);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWorkers() {
        running.set(true);
        log.info("Starting {} notification workers", poolSize);
        for (int i = 0; i < poolSize; i++) {
            final int workerId = i + 1;
            executor.submit(() -> {
                Thread.currentThread().setName("worker-" + workerId);
                log.info("Worker-{} started", workerId);
                while (running.get()) {
                    try {
                        boolean processed = notificationWorker.processNextTask();
                        if (!processed) {
                            Thread.sleep(pollInterval);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("Worker-{} error: {}", workerId, e.getMessage());
                        try {
                            Thread.sleep(pollInterval);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                log.info("Worker-{} stopped", workerId);
            });
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down notification workers...");
        running.set(false);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                log.warn("Workers did not terminate in time, forced shutdown");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Notification workers shut down");
    }
}
