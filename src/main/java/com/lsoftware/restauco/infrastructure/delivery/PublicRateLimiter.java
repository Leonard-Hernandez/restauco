package com.lsoftware.restauco.infrastructure.delivery;

import com.lsoftware.restauco.entity.exception.TooManyRequestsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caps what one visitor (by IP) can do without an account in a window: map lookups cost the
 * public geocoder's goodwill, and orders land on a waiter's screen. In memory, like the SSE
 * hub: fine for one API replica.
 */
@Component
public class PublicRateLimiter {

    private record Window(long start, int count) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public void check(String action, String ip, int max, Duration window) {
        long now = System.currentTimeMillis();
        Window current = windows.compute(action + ":" + ip, (k, w) -> w == null || now - w.start() >= window.toMillis()
                ? new Window(now, 1) : new Window(w.start(), w.count() + 1));
        if (current.count() > max) {
            throw new TooManyRequestsException("TOO_MANY_REQUESTS", "Too many requests, try again in a few minutes");
        }
    }

    @Scheduled(fixedRate = 600_000)
    void prune() {
        long cutoff = System.currentTimeMillis() - Duration.ofHours(1).toMillis();
        windows.values().removeIf(w -> w.start() < cutoff);
    }
}
