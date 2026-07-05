package com.noticore.noticore.repository;

import com.noticore.noticore.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    // Spring Data reads this method name and builds the SQL itself:
    // "SELECT * FROM notifications WHERE user_id = ?"
    // You never write that query by hand.
    List<Notification> findByUserId(String userId);
}
