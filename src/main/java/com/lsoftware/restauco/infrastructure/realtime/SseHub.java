package com.lsoftware.restauco.infrastructure.realtime;

import com.lsoftware.restauco.infrastructure.order.dto.TicketResponse;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Open Server-Sent Event streams, per restaurant:
 * <ul>
 *   <li>staff: waiter and kitchen screens, every ticket and floor change;</li>
 *   <li>table:{id}: guests of a table (QR), only that table's tickets.</li>
 * </ul>
 * Events: {@code ticket} (a TicketResponse), {@code floor} ({tableId}: a bill opened/closed,
 * null for a delivery), {@code menu} (something ran out or came back) and {@code ready} on connect. Clients refetch their screen on (re)connect, so a missed
 * event is never lost for good.
 * <p>
 * Every event also goes to the {@link EventLog}, for clients that poll instead.
 * <p>
 * In memory: fine for one API replica. With several, fan out through Postgres LISTEN/NOTIFY.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseHub {

    static final Duration TIMEOUT = Duration.ofMinutes(30);

    private final Map<String, Set<SseEmitter>> channels = new ConcurrentHashMap<>();
    // Sends never block the request that changed the ticket
    private final ExecutorService sender = Executors.newVirtualThreadPerTaskExecutor();
    private final EventLog events;

    public SseEmitter staff(Long tenantId) {
        return register(staffKey(tenantId));
    }

    public SseEmitter table(Long tenantId, Long tableId) {
        return register(tableKey(tenantId, tableId));
    }

    public EventLog.Batch staffSince(Long tenantId, Long after) {
        return events.since(staffKey(tenantId), after);
    }

    public EventLog.Batch tableSince(Long tenantId, Long tableId, Long after) {
        return events.since(tableKey(tenantId, tableId), after);
    }

    private static String staffKey(Long tenantId) {
        return tenantId + ":staff";
    }

    private static String tableKey(Long tenantId, Long tableId) {
        return tenantId + ":table:" + tableId;
    }

    @TransactionalEventListener(fallbackExecution = true)
    void on(OrderEvents.TicketChanged event) {
        TicketResponse payload = TicketResponse.from(event.ticket());
        publish(staffKey(event.tenantId()), "ticket", payload);
        // Deliveries have no table: their customer polls instead
        if (event.ticket().tableId() != null) {
            publish(tableKey(event.tenantId(), event.ticket().tableId()), "ticket", payload);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    void on(OrderEvents.SessionChanged event) {
        Map<String, Long> payload = new HashMap<>();
        payload.put("tableId", event.tableId());
        publish(staffKey(event.tenantId()), "floor", payload);
        if (event.tableId() != null) {
            publish(tableKey(event.tenantId(), event.tableId()), "floor", payload);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    void on(OrderEvents.MenuChanged event) {
        publish(staffKey(event.tenantId()), "menu", Map.of());
    }

    /** Keeps proxies from closing idle streams and detects dead clients. */
    @Scheduled(fixedRate = 20_000)
    void heartbeat() {
        channels.forEach((key, emitters) -> emitters.forEach(emitter -> sender.execute(() -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException e) {
                drop(key, emitter);
            }
        })));
    }

    private SseEmitter register(String key) {
        SseEmitter emitter = new SseEmitter(TIMEOUT.toMillis());
        channels.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> drop(key, emitter));
        emitter.onTimeout(() -> {
            drop(key, emitter);
            emitter.complete();
        });
        emitter.onError(e -> drop(key, emitter));
        try {
            emitter.send(SseEmitter.event().name("ready").data("{}", MediaType.APPLICATION_JSON).reconnectTime(3000));
        } catch (IOException e) {
            drop(key, emitter);
        }
        return emitter;
    }

    private void publish(String key, String name, Object payload) {
        events.append(key, name, payload);
        Set<SseEmitter> emitters = channels.get(key);
        if (emitters == null) return;
        for (SseEmitter emitter : emitters) {
            sender.execute(() -> {
                try {
                    emitter.send(SseEmitter.event().name(name).data(payload, MediaType.APPLICATION_JSON));
                } catch (IOException | IllegalStateException e) {
                    log.debug("Dropping SSE client on {}: {}", key, e.getMessage());
                    drop(key, emitter);
                }
            });
        }
    }

    private void drop(String key, SseEmitter emitter) {
        channels.computeIfPresent(key, (k, set) -> {
            set.remove(emitter);
            return set.isEmpty() ? null : set;
        });
    }

    @PreDestroy
    void shutdown() {
        channels.values().forEach(set -> set.forEach(SseEmitter::complete));
        sender.shutdownNow();
    }
}
