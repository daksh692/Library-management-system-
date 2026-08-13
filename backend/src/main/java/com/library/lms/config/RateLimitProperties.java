package com.library.lms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Rate-limit thresholds, bound from {@code app.rate-limit.*}.
 *
 * <p>Rules.md #1: every threshold is configuration. Nothing here may be a literal
 * in the filter.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private final Tier auth = new Tier(5, 1);
    private final Tier publicApi = new Tier(30, 1);
    private final Tier authenticated = new Tier(100, 1);

    /** Consecutive auth failures on one account before backoff begins. */
    private int backoffThreshold = 3;
    /** Backoff seconds = base ^ (failures - threshold), capped by maxBackoffSeconds. */
    private int backoffBase = 2;
    private int maxBackoffSeconds = 900;      // 15 minutes
    /** How long a failure record survives without further failures. */
    private int failureWindowMinutes = 30;
    /** Trust X-Forwarded-For. Only enable behind a proxy you control. */
    private boolean trustForwardedHeaders = false;

    public Tier getAuth() { return auth; }
    public Tier getPublicApi() { return publicApi; }
    public Tier getAuthenticated() { return authenticated; }

    public int getBackoffThreshold() { return backoffThreshold; }
    public void setBackoffThreshold(int v) { this.backoffThreshold = v; }
    public int getBackoffBase() { return backoffBase; }
    public void setBackoffBase(int v) { this.backoffBase = v; }
    public int getMaxBackoffSeconds() { return maxBackoffSeconds; }
    public void setMaxBackoffSeconds(int v) { this.maxBackoffSeconds = v; }
    public int getFailureWindowMinutes() { return failureWindowMinutes; }
    public void setFailureWindowMinutes(int v) { this.failureWindowMinutes = v; }
    public boolean isTrustForwardedHeaders() { return trustForwardedHeaders; }
    public void setTrustForwardedHeaders(boolean v) { this.trustForwardedHeaders = v; }

    /** Token-bucket settings for one route class. */
    public static class Tier {
        private int capacity;
        private int refillMinutes;

        public Tier(int capacity, int refillMinutes) {
            this.capacity = capacity;
            this.refillMinutes = refillMinutes;
        }

        public int getCapacity() { return capacity; }
        public void setCapacity(int v) { this.capacity = v; }
        public int getRefillMinutes() { return refillMinutes; }
        public void setRefillMinutes(int v) { this.refillMinutes = v; }
    }
}
