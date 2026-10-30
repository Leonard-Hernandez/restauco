package com.lsoftware.restauco.usecase.voice;

import com.lsoftware.restauco.entity.menu.gateway.DishGateway;
import com.lsoftware.restauco.entity.order.exception.TicketNotFoundException;
import com.lsoftware.restauco.entity.order.gateway.TicketGateway;
import com.lsoftware.restauco.entity.voice.exception.VoiceSampleNotFoundException;
import com.lsoftware.restauco.entity.voice.gateway.VoiceSampleGateway;
import com.lsoftware.restauco.entity.voice.model.VoiceOrder;
import com.lsoftware.restauco.entity.voice.model.VoiceSample;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Orders dictated with the phone's speech-to-text: the text becomes a draft the waiter edits
 * (nothing reaches the kitchen from here). Each one is kept with the round finally sent, so the
 * parser can be measured and a model trained on real orders later.
 */
@Component
@RequiredArgsConstructor
public class TakeVoiceOrderUseCase {

    public static final int MAX_TEXT = 1000;

    private final DishGateway dishes;
    private final VoiceSampleGateway samples;
    private final TicketGateway tickets;
    private final Clock clock;

    public record Understood(Long sampleId, VoiceOrder order) {}

    @Transactional
    public Understood understand(String text, String waiterSub) {
        String said = text.strip();
        if (said.length() > MAX_TEXT) said = said.substring(0, MAX_TEXT);
        VoiceOrder order = new OrderTextParser(dishes.findAll()).parse(said);
        VoiceSample sample = samples.save(new VoiceSample(null, said, order, null, waiterSub, OffsetDateTime.now(clock)));
        return new Understood(sample.id(), order);
    }

    /** The round that came out of a dictated order, after the waiter's edits. */
    @Transactional
    public void sent(Long sampleId, Long ticketId) {
        VoiceSample sample = samples.findById(sampleId).orElseThrow(VoiceSampleNotFoundException::new);
        tickets.findById(ticketId).orElseThrow(TicketNotFoundException::new);
        samples.save(sample.withTicket(ticketId));
    }
}
