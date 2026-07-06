package com.noticore.noticore.ratelimit;

import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.noticore.noticore.repository.UserPreferenceRepository;

@Service
public class RateLimiterService {

    // Used if a user has no row in user_preferences yet -- matches the same
    // default already defined on the UserPreference entity itself in Phase 1.
    private static final int DEFAULT_MAX_PER_MINUTE = 10;
    private static final long WINDOW_MILLIS = 60_000; // 60 seconds

    private final StringRedisTemplate redisTemplate;
    private final UserPreferenceRepository userPreferenceRepository;

    public RateLimiterService(StringRedisTemplate redisTemplate,
                               UserPreferenceRepository userPreferenceRepository) {
        this.redisTemplate = redisTemplate;
        this.userPreferenceRepository = userPreferenceRepository;
    }

    /**
     * Returns true if this user is still within their allowed notifications
     * per minute, and records this attempt if so. Returns false if the user
     * has already hit their limit in the last 60 seconds (a true sliding
     * window, not a fixed clock-minute).
     */
    public boolean isAllowed(String userId) {
        int maxPerMinute = userPreferenceRepository.findById(userId)
                .map(pref -> pref.getMaxNotificationsPerMinute())
                .orElse(DEFAULT_MAX_PER_MINUTE);

        String key = "ratelimit:" + userId;
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MILLIS;

        // Step 1: drop every entry older than the window -- this is what
        // makes it "sliding" rather than fixed: old entries fall off
        // continuously rather than all resetting at once on the clock.
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Step 2: how many requests are left inside the current window?
        Long countInWindow = redisTemplate.opsForZSet().zCard(key);
        if (countInWindow != null && countInWindow >= maxPerMinute) {
            return false; // over the limit -- reject, don't record this attempt
        }

        // Step 3: record this attempt. Score = timestamp (what makes the
        // sliding window math work); member must be unique per call, since a
        // ZSET can't hold two entries with the same member -- a random UUID
        // guarantees that even if two requests land in the same millisecond.
        redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);

        // Housekeeping: let the whole key expire if this user goes quiet,
        // so we don't keep an empty/stale ZSET around forever in Redis.
        redisTemplate.expire(key, java.time.Duration.ofSeconds(120));

        return true;
    }
}