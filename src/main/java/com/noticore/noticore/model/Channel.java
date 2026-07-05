package com.noticore.noticore.model;

import jakarta.persistence.*;
import java.util.UUID;

// Separate from Notification on purpose: a user has ONE registered email/
// phone/device per channel type, but can have MANY notifications sent to it
// over time. Keeping them apart avoids repeating "user's email address" in
// every single notification row.
@Entity
@Table(name = "channels")
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType type;

    @Column(nullable = false)
    private String address; // email address, phone number, or device push token

    private boolean verified = false;

    protected Channel() {}

    public Channel(String userId, ChannelType type, String address) {
        this.userId = userId;
        this.type = type;
        this.address = address;
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public ChannelType getType() { return type; }
    public String getAddress() { return address; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
