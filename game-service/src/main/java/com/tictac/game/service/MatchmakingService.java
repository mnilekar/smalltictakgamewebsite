package com.tictac.game.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MatchmakingService {

    static final int TIMEOUT_SECONDS = 30;

    private final PvpService pvp;
    public MatchmakingService(PvpService pvp) { this.pvp = pvp; }

    private static class Ticket {
        final String id;
        final long userId;
        final Instant expiresAt;
        volatile boolean matched = false;
        volatile PvpService.GameStateDto dto; // result for this user when matched
        Ticket(long userId) {
            this.id = UUID.randomUUID().toString();
            this.userId = userId;
            this.expiresAt = Instant.now().plusSeconds(TIMEOUT_SECONDS);
        }
        boolean expired() { return Instant.now().isAfter(expiresAt); }
    }

    private final ConcurrentLinkedQueue<Ticket> queue = new ConcurrentLinkedQueue<>();
    private final Map<String, Ticket> byId = new ConcurrentHashMap<>();
    private final Map<Long, String> ticketByUser = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public JoinReply join(long userId) {
        // If user already waiting, return same ticket
        var existingId = ticketByUser.get(userId);
        if (existingId != null) {
            var t = byId.get(existingId);
            if (t != null && !t.expired() && !t.matched)
                return JoinReply.waiting(t.id, t.expiresAt);
            // cleanup
            cleanupTicket(existingId);
        }

        lock.lock();
        try {
            // Try to match against someone already waiting
            Ticket partner = null;
            while ((partner = queue.peek()) != null) {
                if (partner.expired() || partner.userId == userId) {
                    queue.poll(); // drop expired or same-user
                    cleanupTicket(partner.id);
                    continue;
                }
                // Found a partner
                queue.poll();
                // Create game: partner is X, current caller is O
                var pair = pvp.startPaired(partner.userId, userId);
                partner.matched = true;
                partner.dto = pair.forX(); // their perspective
                // Create a synthetic ticket for current user (not stored) — just return matched
                return JoinReply.matched(pair.forO());
            }

            // No partner: enqueue new ticket
            Ticket t = new Ticket(userId);
            byId.put(t.id, t);
            ticketByUser.put(userId, t.id);
            queue.add(t);
            return JoinReply.waiting(t.id, t.expiresAt);
        } finally {
            lock.unlock();
        }
    }

    public Optional<JoinReply> status(String ticketId) {
        var t = byId.get(ticketId);
        if (t == null) return Optional.empty();
        if (t.matched) {
            // One-time read is fine; keep it for a short while
            return Optional.of(JoinReply.matched(t.dto));
        }
        if (t.expired()) {
            cleanupTicket(ticketId);
            return Optional.of(JoinReply.timeout());
        }
        return Optional.of(JoinReply.waiting(t.id, t.expiresAt));
    }

    public void cancel(String ticketId) {
        cleanupTicket(ticketId);
    }

    private void cleanupTicket(String ticketId) {
        var t = byId.remove(ticketId);
        if (t != null) {
            queue.remove(t);
            ticketByUser.remove(t.userId, ticketId);
        }
    }

    public static final class JoinReply {
        public final boolean matched;
        public final String ticketId;
        public final String waitUntil; // ISO when waiting
        public final Integer timeoutSeconds;
        public final PvpService.GameStateDto game;

        private JoinReply(boolean matched, String ticketId, String waitUntil, Integer timeoutSeconds, PvpService.GameStateDto game) {
            this.matched = matched; this.ticketId = ticketId; this.waitUntil = waitUntil; this.timeoutSeconds = timeoutSeconds; this.game = game;
        }
        static JoinReply waiting(String id, Instant until) {
            return new JoinReply(false, id, until.toString(), TIMEOUT_SECONDS, null);
        }
        static JoinReply matched(PvpService.GameStateDto dto) {
            return new JoinReply(true, null, null, null, dto);
        }
        static JoinReply timeout() {
            return new JoinReply(false, null, null, TIMEOUT_SECONDS, null);
        }
    }
}
