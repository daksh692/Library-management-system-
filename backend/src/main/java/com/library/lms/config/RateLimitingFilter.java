package com.library.lms.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Per-IP token-bucket rate limiting, tiered by route class.
 *
 * <p>Three tiers, all configured under {@code app.rate-limit.*}: strict on
 * {@code /api/auth/**}, moderate on {@code /api/public/**}, generous on everything
 * authenticated. Per-<em>account</em> backoff is handled separately by
 * {@link com.library.lms.service.LoginAttemptService}, because it keys on the
 * submitted user id rather than the source address.</p>
 *
 * <p>The bucket cache is bounded and self-evicting — the previous
 * {@code ConcurrentHashMap} grew forever, which is itself a slow memory-exhaustion
 * vector.</p>
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RateLimitProperties props;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(50_000)
            .build();

    private enum Tier { AUTH, PUBLIC, AUTHENTICATED }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Tier tier = tierFor(request.getRequestURI());
        String key = tier + ":" + clientIp(request);

        Bucket bucket = buckets.get(key, k -> newBucket(tier));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }

        long retryAfter = settings(tier).getRefillMinutes() * 60L;
        log.warn("Rate limit hit: {} on {}", key, request.getRequestURI());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.getWriter().write("""
                {"error":"Too many requests. Please wait %d seconds and try again.",\
                "code":"RATE_LIMIT_EXCEEDED","status":429,"timestamp":"%s"}"""
                .formatted(retryAfter, Instant.now()));
    }

    private Tier tierFor(String path) {
        if (path.startsWith("/api/auth/"))   return Tier.AUTH;
        if (path.startsWith("/api/public/")) return Tier.PUBLIC;
        return Tier.AUTHENTICATED;
    }

    private RateLimitProperties.Tier settings(Tier tier) {
        return switch (tier) {
            case AUTH          -> props.getAuth();
            case PUBLIC        -> props.getPublicApi();
            case AUTHENTICATED -> props.getAuthenticated();
        };
    }

    private Bucket newBucket(Tier tier) {
        RateLimitProperties.Tier cfg = settings(tier);
        Bandwidth limit = Bandwidth.classic(
                cfg.getCapacity(),
                Refill.greedy(cfg.getCapacity(), Duration.ofMinutes(cfg.getRefillMinutes())));
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Resolves the caller's address.
     *
     * <p>{@code X-Forwarded-For} is trusted only when
     * {@code app.rate-limit.trust-forwarded-headers=true}, because the header is
     * client-supplied and trivially spoofed unless a proxy you control overwrites it.
     * Behind nginx or a load balancer, enable it — otherwise every request appears
     * to come from the proxy and the whole site shares one bucket.</p>
     */
    private String clientIp(HttpServletRequest request) {
        if (props.isTrustForwardedHeaders()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();   // left-most is the origin client
            }
        }
        return request.getRemoteAddr();
    }
}
