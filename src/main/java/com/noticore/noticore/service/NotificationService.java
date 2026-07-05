package com.noticore.noticore.service;

import com.noticore.noticore.model.ChannelType;
import com.noticore.noticore.model.Notification;
import com.noticore.noticore.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // Constructor injection again -- Spring builds this bean and automatically
    // hands it the NotificationRepository bean it built earlier.
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification createNotification(String userId, ChannelType channel,
                                             String recipient, String message) {
        Notification notification = new Notification(userId, channel, recipient, message);
        // Right now this just saves as PENDING and stops. In Phase 2, right
        // after this save(), we'll add: kafkaTemplate.send(topicFor(channel), notification.getId())
        // -- that's the ONLY change this method needs later. Everything else
        // (validation, building the object) stays exactly the same.
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForUser(String userId) {
        return notificationRepository.findByUserId(userId);
    }

    public Notification getNotification(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + id));
    }
}
