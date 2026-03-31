package de.evia.travelmate.trips.adapters.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "accommodation_candidate")
public class AccommodationCandidateJpaEntity {

    @Id
    @Column(name = "candidate_id")
    private UUID candidateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_poll_id", nullable = false)
    private AccommodationPollJpaEntity poll;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "url")
    private String url;

    @Column(name = "description")
    private String description;

    @Column(name = "rooms_json", columnDefinition = "text")
    private String roomsJson;

    protected AccommodationCandidateJpaEntity() {
    }

    public AccommodationCandidateJpaEntity(final UUID candidateId, final AccommodationPollJpaEntity poll,
                                           final String name, final String url, final String description,
                                           final String roomsJson) {
        this.candidateId = candidateId;
        this.poll = poll;
        this.name = name;
        this.url = url;
        this.description = description;
        this.roomsJson = roomsJson;
    }

    public UUID getCandidateId() { return candidateId; }
    public AccommodationPollJpaEntity getPoll() { return poll; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getDescription() { return description; }
    public String getRoomsJson() { return roomsJson; }
    public void setRoomsJson(final String roomsJson) { this.roomsJson = roomsJson; }
}
