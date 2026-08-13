package com.library.lms.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.library.lms.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-account exponential backoff for failed authentication.
 *
 * <p>Rules.md #1 asks for backoff rather than a hard lockout, and for the limit to
 * be per-account as well as per-IP. A hard lockout is itself a denial-of-service:
 * anyone who knows a username can lock its owner out. Backoff makes brute force
 * impractical while leaving the real owner able to get in after a short wait.</p>
 *
 * <p>Delay grows as {@code base ^ (failures - threshold)} seconds:
 * with the defaults, failure 4 waits 2s, 5 waits 4s, 6 waits 8s, and so on to a
 * 15-minute ceiling.</p>
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final RateLimitProperties props;

    private Cache<String, Attempt> attempts;

    private Cache<String, Attempt> cache() {
        if (attempts == null) {
            attempts = Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(props.getFailureWindowMinutes()))
                    .maximumSize(100_000)      // bounded — cannot be grown by an attacker
                    .build();
        }
        return attempts;
    }

    /** Mutable failure record for one account. */
    private static class Attempt {
        final AtomicInteger count = new AtomicInteger(0);
        volatile Instant blockedUntil = Instant.EPOCH;
    }

    /** @return seconds the caller must wait, or 0 if they may proceed */
    public long secondsUntilAllowed(String userId) {
        Attempt attempt = cache().getIfPresent(key(userId));
        if (attempt == null) return 0;

        long wait = Instant.now().until(attempt.blockedUntil, java.time.temporal.ChronoUnit.SECONDS);
        return Math.max(0, wait);
    }

    /** Records a failure and extends the backoff window. */
    public void recordFailure(String userId) {
        Attempt attempt = cache().get(key(userId), k -> new Attempt());
        int failures = attempt.count.incrementAndGet();

        if (failures > props.getBackoffThreshold()) {
            long seconds = Math.min(
                    (long) Math.pow(props.getBackoffBase(), failures - props.getBackoffThreshold()),
                    props.getMaxBackoffSeconds());

            attempt.blockedUntil = Instant.now().plusSeconds(seconds);

            log.warn("Account {} has {} consecutive failures; backing off {}s",
                    userId, failures, seconds);
        }
    }

    /** Clears the record on a successful login. */
    public void recordSuccess(String userId) {
        cache().invalidate(key(userId));
    }

    private String key(String userId) {
        return userId == null ? "" : userId.trim().toLowerCase();
    }
}
