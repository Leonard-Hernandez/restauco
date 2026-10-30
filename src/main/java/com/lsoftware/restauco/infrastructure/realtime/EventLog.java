package com.lsoftware.restauco.infrastructure.realtime;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The last few minutes of events per channel, for clients that cannot keep a stream open
 * (Cloudflare quick tunnels do not pass Server-Sent Events through): they poll with the
 * cursor of the last batch and get what happened since, or {@code reset} when that is no
 * longer known (pruned, or the API restarted) and they must refetch their screen.
 * <p>
 * Cursors start at the boot time in microseconds, so they keep growing across restarts.
 */
@Component
public class EventLog {

    static final Duration RETENTION = Duration.ofMinutes(5);
    static final int MAX_PER_CHANNEL = 200;

    public record Event(String name, Object data) {}

    public record Batch(long cursor, boolean reset, List<Event> events) {}

    private record Entry(long seq, Instant at, Event event) {}

    private static final class Channel {
        final Deque<Entry> entries = new ArrayDeque<>();
        /** Every event at or below this cursor is gone */
        long pruned;

        Channel(long start) {
            pruned = start;
        }
    }

    private final long start = Instant.now().toEpochMilli() * 1000;
    private long seq = start;
    private final Map<String, Channel> channels = new ConcurrentHashMap<>();

    public synchronized void append(String key, String name, Object data) {
        Channel channel = channels.computeIfAbsent(key, k -> new Channel(start));
        channel.entries.addLast(new Entry(++seq, Instant.now(), new Event(name, data)));
        prune(channel);
    }

    /** Events after {@code after}; with no cursor, just the current one to start from. */
    public synchronized Batch since(String key, Long after) {
        Channel channel = channels.get(key);
        if (after == null || after > seq) return new Batch(seq, true, List.of());
        if (channel == null) return new Batch(seq, after < start, List.of());
        prune(channel);
        if (after < channel.pruned) return new Batch(seq, true, List.of());
        List<Event> events = new ArrayList<>();
        for (Entry e : channel.entries) {
            if (e.seq() > after) events.add(e.event());
        }
        return new Batch(seq, false, events);
    }

    private void prune(Channel channel) {
        Instant cutoff = Instant.now().minus(RETENTION);
        while (!channel.entries.isEmpty()
                && (channel.entries.size() > MAX_PER_CHANNEL || channel.entries.peekFirst().at().isBefore(cutoff))) {
            channel.pruned = channel.entries.removeFirst().seq();
        }
    }
}
