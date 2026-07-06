package com.noticore.noticore.model;

// An enum = a fixed, closed list of valid values. Java won't let you assign
// anything outside this list to a NotificationStatus field — no typos like
// "Pending " with a trailing space silently breaking a status check later.
//
// This maps directly to the flow diagram: every notification starts PENDING,
// then becomes SENT (success) or moves through RETRYING into FAILED
// (Spring Retry exhausted its attempts, message went to the dead letter queue).
public enum NotificationStatus {
    PENDING,
    RETRYING,
    SENT,
    FAILED,
    CANCELLED
}