package com.noticore.noticore.service;

import com.noticore.noticore.model.ChannelType;
import com.noticore.noticore.model.Notification;
import com.noticore.noticore.model.NotificationStatus;
import com.noticore.noticore.repository.NotificationRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // KafkaTemplate is Spring's wrapper for "being a producer" -- calling
    // .send(topic, message) on it publishes a message to that Kafka topic.
    // Just like NotificationRepository, Spring builds one KafkaTemplate bean
    // automatically (using the bootstrap-servers config from application.yml)
    // and hands it to us here via constructor injection -- same DI pattern
    // as always, just a new type of dependency.
    private final KafkaTemplate<String, String> kafkaTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                                KafkaTemplate<String, String> kafkaTemplate) {
        this.notificationRepository = notificationRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Notification createNotification(String userId, ChannelType channel,
                                             String recipient, String message) {
        Notification notification = new Notification(userId, channel, recipient, message);
        Notification saved = notificationRepository.save(notification);

        // THE NEW LINE. Everything above is unchanged from Phase 1.
        // We publish just the notification's ID as a plain string -- the
        // consumer will use that ID to go look up the full notification from
        // Supabase itself. We send the ID rather than the whole object to
        // keep the message small and avoid needing a JSON serializer yet.
        kafkaTemplate.send(topicFor(channel), saved.getId().toString());

        return saved;
    }

    // Builds a topic name like "notifications.email" from the ChannelType
    // enum. This is the routing rule: EMAIL -> notifications.email,
    // SMS -> notifications.sms, PUSH -> notifications.push.
    private String topicFor(ChannelType channel) {
        return "notifications." + channel.name().toLowerCase();
    }

    public List<Notification> getNotificationsForUser(String userId) {
        return notificationRepository.findByUserId(userId);
    }

    public Notification getNotification(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + id));
    }

    // Business rule: only a notification still sitting at PENDING can be
    // cancelled -- once it's RETRYING, SENT, or FAILED, cancelling no longer
    // makes sense (it's already being processed, already succeeded, or
    // already gave up). Trying to cancel anything else is rejected.
    public Notification cancelNotification(UUID id) {
        Notification notification = getNotification(id);
        if (notification.getStatus() != NotificationStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING notifications can be cancelled (current status: "
                            + notification.getStatus() + ")");
        }
        notification.setStatus(NotificationStatus.CANCELLED);
        return notificationRepository.save(notification);
    }

    // Business rule: only a notification that's FAILED (i.e. sitting in the
    // dead letter queue) makes sense to replay. Replaying resets it back to
    // PENDING and republishes it to its original channel topic -- exactly
    // the same publish step createNotification() does, just for a
    // notification that already exists rather than a brand new one.
    public Notification replayNotification(UUID id) {
        Notification notification = getNotification(id);
        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new IllegalStateException(
                    "Only FAILED notifications can be replayed (current status: "
                            + notification.getStatus() + ")");
        }
        notification.setStatus(NotificationStatus.PENDING);
        Notification saved = notificationRepository.save(notification);
        kafkaTemplate.send(topicFor(saved.getChannel()), saved.getId().toString());
        return saved;
    }
}