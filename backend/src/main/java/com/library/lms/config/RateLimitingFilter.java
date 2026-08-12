package com.library.lms.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private final LibraryProperties props;

    public RateLimitingFilter(LibraryProperties props) {
        this.props = props;
    }

    private Bucket createNewBucket(String path) {
        if (path.startsWith("/api/auth/")) {
            // Strict limit for auth from configuration
            LibraryProperties.RateLimit.Tier tier = props.getRateLimit().getAuth();
            Bandwidth limit = Bandwidth.classic(tier.getCapacity(), Refill.greedy(tier.getCapacity(), Duration.ofMinutes(tier.getRefillMinutes())));
            return Bucket.builder().addLimit(limit).build();
        } else if (path.startsWith("/api/public/")) {
            // Moderate limit for public endpoints from configuration
            LibraryProperties.RateLimit.Tier tier = props.getRateLimit().getPublicApi();
            Bandwidth limit = Bandwidth.classic(tier.getCapacity(), Refill.greedy(tier.getCapacity(), Duration.ofMinutes(tier.getRefillMinutes())));
            return Bucket.builder().addLimit(limit).build();
        } else {
            // Looser limit for authenticated user actions from configuration
            LibraryProperties.RateLimit.Tier tier = props.getRateLimit().getAuthenticated();
            Bandwidth limit = Bandwidth.classic(tier.getCapacity(), Refill.greedy(tier.getCapacity(), Duration.ofMinutes(tier.getRefillMinutes())));
            return Bucket.builder().addLimit(limit).build();
        }
    }

    private Bucket resolveBucket(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        
        // Group limits based on route type instead of specific path if needed, 
        // here we use IP + general route type
        String bucketKey = ip + "-" + (path.startsWith("/api/auth/") ? "auth" : path.startsWith("/api/public/") ? "public" : "general");
        
        return cache.computeIfAbsent(bucketKey, k -> createNewBucket(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        Bucket bucket = resolveBucket(request);
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests. Please try again later.");
        }
    }
}
