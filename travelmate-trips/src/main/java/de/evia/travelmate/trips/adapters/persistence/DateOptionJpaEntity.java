package de.evia.travelmate.trips.adapters.persistence;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "date_option")
public class DateOptionJpaEntity {

    @Id
    @Column(name = "date_option_id")
    private UUID dateOptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "date_poll_id", nullable = false)
    private DatePollJpaEntity datePoll;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    protected DateOptionJpaEntity() {
    }

    public DateOptionJpaEntity(final UUID dateOptionId, final DatePollJpaEntity datePoll,
                               final LocalDate startDate, final LocalDate endDate) {
        this.dateOptionId = dateOptionId;
        this.datePoll = datePoll;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public UUID getDateOptionId() { return dateOptionId; }
    public DatePollJpaEntity getDatePoll() { return datePoll; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}
