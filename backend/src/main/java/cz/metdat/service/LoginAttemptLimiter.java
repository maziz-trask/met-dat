package cz.metdat.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Slows down password guessing: at most 5 failed sign-ins per account within 10 minutes (in-memory, per instance). */
@Component
public class LoginAttemptLimiter {
    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(10);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptLimiter(Clock clock) { this.clock = clock; }

    public boolean isBlocked(String account) {
        Deque<Instant> q = failures.get(key(account));
        if (q == null) return false;
        synchronized (q) {
            prune(q);
            return q.size() >= MAX_FAILURES;
        }
    }

    public void recordFailure(String account) {
        Deque<Instant> q = failures.computeIfAbsent(key(account), k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q);
            q.addLast(Instant.now(clock));
        }
    }

    public void reset(String account) { failures.remove(key(account)); }

    private void prune(Deque<Instant> q) {
        Instant cutoff = Instant.now(clock).minus(WINDOW);
        while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.removeFirst();
    }

    private static String key(String account) { return account == null ? "" : account.trim().toLowerCase(); }
}
