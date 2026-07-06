package com.noticore.noticore.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// A dedicated listener for the dead letter topic. For now this only logs --
// Phase 5's CLI is where a human-facing "replay this notification" command
// will actually re-publish these IDs back onto their original channel topic.
@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    @KafkaListener(topics = "notifications.dlq", groupId = "dlq-monitor-group")
    public void consumeDeadLetter(String notificationId) {
        log.warn("[DEAD LETTER QUEUE] Notification {} exhausted all retries and needs manual attention",
                notificationId);
    }
}