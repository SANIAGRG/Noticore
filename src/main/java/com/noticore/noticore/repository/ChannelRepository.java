package com.noticore.noticore.repository;

import com.noticore.noticore.model.Channel;
import com.noticore.noticore.model.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {
    // "find the channel row for this user, on this specific channel type"
    // e.g. findByUserIdAndType("user123", ChannelType.EMAIL)
    Optional<Channel> findByUserIdAndType(String userId, ChannelType type);
}
