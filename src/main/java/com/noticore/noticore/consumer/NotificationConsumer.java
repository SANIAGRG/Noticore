package com.noticore.noticore.consumer;

import com.noticore.noticore.model.Notification;
import com.noticore.noticore.ratelimit.RateLimiterService;
import com.noticore.noticore.repository.NotificationRepository;
import com.noticore.noticore.sender.NotificationSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private static final Duration DEDUP_WINDOW = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final NotificationRepository notificationRepository;
    private final RateLimiterService rateLimiterService;
    private final NotificationSenderService notificationSenderService;

    public NotificationConsumer(StringRedisTemplate redisTemplate,
                                 NotificationRepository notificationRepository,
                                 RateLimiterService rateLimiterService,
                                 NotificationSenderService notificationSenderService) {
        this.redisTemplate = redisTemplate;
        this.notificationRepository = notificationRepository;
        this.rateLimiterService = rateLimiterService;
        this.notificationSenderService = notificationSenderService;
    }

    @KafkaListener(topics = "notifications.email", groupId = "email-processor-group")
    public void consumeEmail(String notificationId) {
        process("EMAIL CONSUMER", "email", notificationId);
    }

    @KafkaListener(topics = "notifications.sms", groupId = "sms-processor-group")
    public void consumeSms(String notificationId) {
        process("SMS CONSUMER", "sms", notificationId);
    }

    @KafkaListener(topics = "notifications.push", groupId = "push-processor-group")
    public void consumePush(String notificationId) {
        process("PUSH CONSUMER", "push", notificationId);
    }

    // Dedup -> lookup -> rate limit -> ACTUALLY SEND (new this phase).
    // Note there's no try/catch here around notificationSenderService.send():
    // that method's @Recover handles the fully-failed case internally and
    // returns normally, so as far as this consumer is concerned, send()
    // never throws -- it either succeeds or gracefully hands off to the DLQ.
    private void process(String label, String channel, String notificationId) {
        if (isDuplicate(channel, notificationId)) {
            log.info("[{}] Duplicate detected, skipping notification ID: {}", label, notificationId);
            return;
        }

        Notification notification = notificationRepository.findById(UUID.fromString(notificationId))
                .orElse(null);
        if (notification == null) {
            log.warn("[{}] Notification ID {} not found in database, skipping", label, notificationId);
            return;
        }

        if (!rateLimiterService.isAllowed(notification.getUserId())) {
            log.info("[{}] Rate limit exceeded for user {}, rejecting notification ID: {}",
                    label, notification.getUserId(), notificationId);
            return;
        }

        log.info("[{}] Handing off notification {} to sender", label, notificationId);
        notificationSenderService.send(notification);
    }

    private boolean isDuplicate(String channel, String notificationId) {
        String key = "dedup:" + channel + ":" + notificationId;
        Boolean firstTimeSeen = redisTemplate.opsForValue()
                .setIfAbsent(key, "processed", DEDUP_WINDOW);
        return !Boolean.TRUE.equals(firstTimeSeen);
    }
}