package com.noticore.noticore.sender;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.noticore.noticore.model.Notification;
import com.noticore.noticore.model.NotificationStatus;
import com.noticore.noticore.repository.NotificationRepository;

@Service
public class NotificationSenderService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSenderService.class);
    private static final String DEAD_LETTER_TOPIC = "notifications.dlq";

    private final NotificationRepository notificationRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Used only to simulate real-world send failures, since the real Resend
    // API integration comes later. A genuine email/SMS provider call would
    // sit here instead of this random check.
    private final Random random = new Random();

    public NotificationSenderService(NotificationRepository notificationRepository,
                                      KafkaTemplate<String, String> kafkaTemplate) {
        this.notificationRepository = notificationRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // @Retryable wraps this whole method: if it throws an exception, Spring
    // catches it and calls the method again automatically -- up to
    // maxAttempts times total (so 2 RETRIES after the first try).
    // @Backoff controls the wait between attempts: delay=1000 means wait
    // 1 second before the 2nd attempt, then multiplier=2 doubles it each
    // time (1s, then 2s) -- this is the "exponential backoff" from the
    // original project spec, so a flaky provider gets progressively more
    // breathing room instead of being hammered instantly 3 times in a row.
    @Retryable(retryFor = RuntimeException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public void send(Notification notification) {
        notification.incrementAttemptCount();
        notification.setStatus(NotificationStatus.RETRYING);
        notificationRepository.save(notification);

        log.info("Attempting to send notification {} (attempt {})",
                notification.getId(), notification.getAttemptCount());

        // Simulated ~50% failure rate, standing in for a real, occasionally
        // flaky email/SMS/push provider call.
        if (random.nextBoolean()) {
            throw new RuntimeException("Simulated send failure for notification " + notification.getId());
        }

        notification.setStatus(NotificationStatus.SENT);
        notificationRepository.save(notification);
        log.info("Successfully sent notification {}", notification.getId());
    }

    // @Recover only runs once @Retryable has exhausted ALL attempts and the
    // method still failed every time. Its first parameter must be the
    // exception type that was thrown; remaining parameters must match the
    // original method's parameters. This is where we give up gracefully:
    // mark the notification FAILED and publish it to a dead letter topic,
    // rather than losing it silently.
    @Recover
    public void recover(RuntimeException e, Notification notification) {
        log.error("All retry attempts exhausted for notification {} -- moving to dead letter queue",
                notification.getId());

        notification.setStatus(NotificationStatus.FAILED);
        notificationRepository.save(notification);

        kafkaTemplate.send(DEAD_LETTER_TOPIC, notification.getId().toString());
    }
}