package com.noticore.noticore.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

// Same @Entity pattern as before -- this class = the "notifications" table.
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    // UUID instead of an auto-increment number. Why: in a distributed system
    // with Kafka producers/consumers, you don't want ID collisions or to leak
    // "how many notifications have ever been sent" by guessing sequential IDs.
    private UUID id;

    @Column(nullable = false)
    private String userId; // which user this notification is for

    @Enumerated(EnumType.STRING)
    // Without EnumType.STRING, JPA stores enums as plain integers (0,1,2,3) --
    // unreadable in the DB and dangerous if you ever reorder the enum values.
    // STRING stores the literal word "PENDING" / "SENT" etc in the column.
    @Column(nullable = false)
    private ChannelType channel;

    @Column(nullable = false)
    private String recipient; // the actual email address / phone number / device token used

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    private int attemptCount = 0; // will be incremented by Spring Retry in Phase 4

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    protected Notification() {} // required by JPA

    public Notification(String userId, ChannelType channel, String recipient, String message) {
        this.userId = userId;
        this.channel = channel;
        this.recipient = recipient;
        this.message = message;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public ChannelType getChannel() { return channel; }
    public String getRecipient() { return recipient; }
    public String getMessage() { return message; }
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    public int getAttemptCount() { return attemptCount; }
    public void incrementAttemptCount() { this.attemptCount++; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
