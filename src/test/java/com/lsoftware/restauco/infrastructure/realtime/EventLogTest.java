package com.lsoftware.restauco.infrastructure.realtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventLogTest {

    private final EventLog log = new EventLog();

    @Test
    void firstPollOnlyGivesTheCursor() {
        log.append("1:staff", "ticket", "old");

        EventLog.Batch batch = log.since("1:staff", null);

        assertThat(batch.reset()).isTrue();
        assertThat(batch.events()).isEmpty();
    }

    @Test
    void returnsWhatHappenedAfterTheCursorOnThatChannel() {
        long cursor = log.since("1:staff", null).cursor();
        log.append("1:staff", "ticket", "a");
        log.append("1:table:4", "ticket", "other table");
        log.append("1:staff", "floor", "b");

        EventLog.Batch batch = log.since("1:staff", cursor);

        assertThat(batch.reset()).isFalse();
        assertThat(batch.events()).extracting(EventLog.Event::data).containsExactly("a", "b");
        assertThat(log.since("1:staff", batch.cursor()).events()).isEmpty();
    }

    @Test
    void quietChannelHasNothingNew() {
        long cursor = log.since("1:table:9", null).cursor();

        EventLog.Batch batch = log.since("1:table:9", cursor);

        assertThat(batch.reset()).isFalse();
        assertThat(batch.events()).isEmpty();
    }

    @Test
    void cursorFromTheFutureMeansTheApiRestarted() {
        long cursor = log.since("1:staff", null).cursor();

        assertThat(log.since("1:staff", cursor + 1000).reset()).isTrue();
    }

    @Test
    void prunedEventsForceARefetch() {
        long cursor = log.since("1:staff", null).cursor();
        for (int i = 0; i < EventLog.MAX_PER_CHANNEL + 1; i++) log.append("1:staff", "ticket", i);

        assertThat(log.since("1:staff", cursor).reset()).isTrue();
    }
}
