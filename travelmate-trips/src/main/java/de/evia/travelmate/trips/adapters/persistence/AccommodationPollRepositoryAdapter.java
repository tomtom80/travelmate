package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.common.domain.TenantId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationCandidate;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationCandidateId;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPoll;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollId;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollRepository;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollStatus;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationVote;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationVoteId;
import de.evia.travelmate.trips.domain.accommodationpoll.Amenity;
import de.evia.travelmate.trips.domain.accommodationpoll.CandidateRoom;
import de.evia.travelmate.trips.domain.trip.TripId;

@Repository
public class AccommodationPollRepositoryAdapter implements AccommodationPollRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final AccommodationPollJpaRepository jpaRepository;

    public AccommodationPollRepositoryAdapter(final AccommodationPollJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AccommodationPoll save(final AccommodationPoll poll) {
        final AccommodationPollJpaEntity entity = jpaRepository.findById(poll.accommodationPollId().value())
            .orElseGet(() -> new AccommodationPollJpaEntity(
                poll.accommodationPollId().value(),
                poll.tenantId().value(),
                poll.tripId().value(),
                poll.status().name()
            ));
        entity.setStatus(poll.status().name());
        entity.setSelectedCandidateId(
            poll.selectedCandidateId() != null ? poll.selectedCandidateId().value() : null
        );
        entity.setLastFailedCandidateId(
            poll.lastFailedCandidateId() != null ? poll.lastFailedCandidateId().value() : null
        );
        entity.setLastFailedCandidateNote(poll.lastFailedCandidateNote());
        syncCandidates(entity, poll);
        syncVotes(entity, poll);
        jpaRepository.save(entity);
        return poll;
    }

    @Override
    public Optional<AccommodationPoll> findById(final TenantId tenantId, final AccommodationPollId pollId) {
        return jpaRepository.findByTenantIdAndAccommodationPollId(tenantId.value(), pollId.value())
            .map(this::toDomain);
    }

    @Override
    public Optional<AccommodationPoll> findOpenByTripId(final TenantId tenantId, final TripId tripId) {
        return jpaRepository.findByTenantIdAndTripId(tenantId.value(), tripId.value()).stream()
            .filter(e -> AccommodationPollStatus.OPEN.name().equals(e.getStatus()))
            .findFirst()
            .map(this::toDomain);
    }

    @Override
    public Optional<AccommodationPoll> findLatestByTripId(final TenantId tenantId, final TripId tripId) {
        return jpaRepository.findByTenantIdAndTripId(tenantId.value(), tripId.value()).stream()
            .max(Comparator.comparing(AccommodationPollJpaEntity::getAccommodationPollId))
            .map(this::toDomain);
    }

    @Override
    public void delete(final AccommodationPoll poll) {
        jpaRepository.deleteById(poll.accommodationPollId().value());
    }

    private void syncCandidates(final AccommodationPollJpaEntity entity, final AccommodationPoll poll) {
        final Set<java.util.UUID> currentIds = poll.candidates().stream()
            .map(c -> c.candidateId().value())
            .collect(Collectors.toSet());

        entity.getCandidates().removeIf(e -> !currentIds.contains(e.getCandidateId()));

        for (final AccommodationCandidate candidate : poll.candidates()) {
            final boolean exists = entity.getCandidates().stream()
                .anyMatch(e -> e.getCandidateId().equals(candidate.candidateId().value()));
            if (!exists) {
                final AccommodationCandidateJpaEntity candidateEntity = new AccommodationCandidateJpaEntity(
                    candidate.candidateId().value(), entity,
                    candidate.name(), candidate.url(), candidate.description(),
                    serializeRooms(candidate.rooms()),
                    serializeAmenities(candidate.amenities())
                );
                entity.getCandidates().add(candidateEntity);
            } else {
                entity.getCandidates().stream()
                    .filter(e -> e.getCandidateId().equals(candidate.candidateId().value()))
                    .findFirst()
                    .ifPresent(e -> {
                        e.setRoomsJson(serializeRooms(candidate.rooms()));
                        e.setAmenitiesJson(serializeAmenities(candidate.amenities()));
                    });
            }
        }
    }

    private void syncVotes(final AccommodationPollJpaEntity entity, final AccommodationPoll poll) {
        final Set<java.util.UUID> currentVoteIds = poll.votes().stream()
            .map(v -> v.voteId().value())
            .collect(Collectors.toSet());

        entity.getVotes().removeIf(e -> !currentVoteIds.contains(e.getVoteId()));

        for (final AccommodationVote vote : poll.votes()) {
            final Optional<AccommodationVoteJpaEntity> existing = entity.getVotes().stream()
                .filter(e -> e.getVoteId().equals(vote.voteId().value()))
                .findFirst();

            if (existing.isPresent()) {
                existing.get().setSelectedCandidateId(vote.selectedCandidateId().value());
            } else {
                entity.getVotes().add(new AccommodationVoteJpaEntity(
                    vote.voteId().value(), entity,
                    vote.voterId(), vote.selectedCandidateId().value()
                ));
            }
        }
    }

    private AccommodationPoll toDomain(final AccommodationPollJpaEntity entity) {
        final var candidates = entity.getCandidates().stream()
            .map(c -> new AccommodationCandidate(
                new AccommodationCandidateId(c.getCandidateId()),
                c.getName(), c.getUrl(), c.getDescription(),
                readRooms(c.getRoomsJson()),
                readAmenities(c.getAmenitiesJson())
            ))
            .toList();

        final var votes = entity.getVotes().stream()
            .map(v -> new AccommodationVote(
                new AccommodationVoteId(v.getVoteId()),
                v.getVoterId(),
                new AccommodationCandidateId(v.getSelectedCandidateId())
            ))
            .toList();

        return new AccommodationPoll(
            new AccommodationPollId(entity.getAccommodationPollId()),
            new TenantId(entity.getTenantId()),
            new TripId(entity.getTripId()),
            AccommodationPollStatus.valueOf(entity.getStatus()),
            candidates,
            votes,
            entity.getSelectedCandidateId() != null
                ? new AccommodationCandidateId(entity.getSelectedCandidateId())
                : null,
            entity.getLastFailedCandidateId() != null
                ? new AccommodationCandidateId(entity.getLastFailedCandidateId())
                : null,
            entity.getLastFailedCandidateNote()
        );
    }

    private static String serializeRooms(final List<CandidateRoom> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(rooms);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to serialize candidate rooms", e);
        }
    }

    private static List<CandidateRoom> readRooms(final String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<CandidateRoom>>() {});
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to parse candidate rooms", e);
        }
    }

    private static String serializeAmenities(final Set<Amenity> amenities) {
        if (amenities == null || amenities.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(amenities);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to serialize candidate amenities", e);
        }
    }

    private static Set<Amenity> readAmenities(final String json) {
        if (json == null || json.isBlank()) {
            return Set.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Set<Amenity>>() {});
        } catch (final Exception e) {
            return Set.of();
        }
    }
}
