package de.evia.travelmate.iam.adapters.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.common.events.trips.ParticipantRemovedFromTrip;
import de.evia.travelmate.iam.domain.tripparticipation.TripParticipationRepository;

@Component
@Profile("!test")
public class TripParticipationEventConsumer {

    private final TripParticipationRepository tripParticipationRepository;
    private final Timer participantJoinedTimer;
    private final Timer participantRemovedTimer;

    public TripParticipationEventConsumer(final TripParticipationRepository tripParticipationRepository,
                                          final MeterRegistry meterRegistry) {
        this.tripParticipationRepository = tripParticipationRepository;
        this.participantJoinedTimer = Timer.builder("travelmate.event.processing")
            .tag("scs", "iam")
            .tag("event", "ParticipantJoinedTrip")
            .description("Time spent processing domain events")
            .register(meterRegistry);
        this.participantRemovedTimer = Timer.builder("travelmate.event.processing")
            .tag("scs", "iam")
            .tag("event", "ParticipantRemovedFromTrip")
            .description("Time spent processing domain events")
            .register(meterRegistry);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_PARTICIPANT_JOINED)
    public void onParticipantJoined(final ParticipantJoinedTrip event) {
        participantJoinedTimer.record(() ->
            tripParticipationRepository.add(event.participantId(), event.tripId()));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_PARTICIPANT_REMOVED)
    public void onParticipantRemoved(final ParticipantRemovedFromTrip event) {
        participantRemovedTimer.record(() ->
            tripParticipationRepository.remove(event.participantId(), event.tripId()));
    }
}
