package com.noticore.noticore.controller;

import com.noticore.noticore.model.Notification;
import com.noticore.noticore.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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

    @PatchMapping("/{id}/cancel")
    public Notification cancel(@PathVariable UUID id) {
        return notificationService.cancelNotification(id);
    }

    @PostMapping("/{id}/replay")
    public Notification replay(@PathVariable UUID id) {
        return notificationService.replayNotification(id);
    }

    // Without this, an IllegalStateException thrown by cancelNotification()
    // or replayNotification() (e.g. "already SENT, can't cancel") would
    // surface to the caller as a generic, unhelpful 500 Internal Server
    // Error. @ExceptionHandler intercepts it here and turns it into a
    // proper 409 Conflict with the actual reason as the response body --
    // a client (the CLI, Postman, anything) can then show that message
    // directly instead of a stack trace.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }
}