package com.noticore.noticore.model;

import jakarta.persistence.*;

// This table doesn't get used until Phase 3 (Redis rate limiting), but we
// define it now because it lives right alongside the other two tables in the
// same schema. userId is the @Id here (not a generated UUID) because there's
// exactly one preference row per user -- the userId itself is the natural key.
@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    private String userId;

    // How many notifications this user can receive per minute before the
    // Redis rate limiter (Phase 3) starts rejecting further sends.
    private int maxNotificationsPerMinute = 10;

    private boolean emailOptOut = false;
    private boolean smsOptOut = false;
    private boolean pushOptOut = false;

    protected UserPreference() {}

    public UserPreference(String userId) {
        this.userId = userId;
    }

    public String getUserId() { return userId; }
    public int getMaxNotificationsPerMinute() { return maxNotificationsPerMinute; }
    public void setMaxNotificationsPerMinute(int max) { this.maxNotificationsPerMinute = max; }
    public boolean isEmailOptOut() { return emailOptOut; }
    public void setEmailOptOut(boolean v) { this.emailOptOut = v; }
    public boolean isSmsOptOut() { return smsOptOut; }
    public void setSmsOptOut(boolean v) { this.smsOptOut = v; }
    public boolean isPushOptOut() { return pushOptOut; }
    public void setPushOptOut(boolean v) { this.pushOptOut = v; }
}
