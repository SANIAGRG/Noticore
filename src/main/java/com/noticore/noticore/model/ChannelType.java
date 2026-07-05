package com.noticore.noticore.model;

// This single enum is what will decide, in Phase 2, WHICH Kafka topic a
// notification gets published to: EMAIL -> "notifications.email",
// SMS -> "notifications.sms", PUSH -> "notifications.push".
public enum ChannelType {
    EMAIL,
    SMS,
    PUSH
}
