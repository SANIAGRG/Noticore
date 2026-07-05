package com.noticore.noticore.controller;

import com.noticore.noticore.model.Notification;
import com.noticore.noticore.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public Notification create(@Valid @RequestBody CreateNotificationRequest req) {
        // @Valid triggers the @NotBlank/@NotNull checks from the DTO above --
        // if userId is missing, Spring auto-returns a 400 error before this
        // method body even runs. You don't write that check yourself.
        return notificationService.createNotification(
                req.userId, req.channel, req.recipient, req.message);
    }

    @GetMapping("/user/{userId}")
    public List<Notification> getForUser(@PathVariable String userId) {
        return notificationService.getNotificationsForUser(userId);
    }

    @GetMapping("/{id}")
    public Notification getOne(@PathVariable UUID id) {
        return notificationService.getNotification(id);
    }
}
