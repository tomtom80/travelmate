ALTER TABLE accommodation_poll
    ADD COLUMN last_failed_candidate_id UUID,
    ADD COLUMN last_failed_candidate_note VARCHAR(2000);

UPDATE accommodation_poll
SET status = 'BOOKED'
WHERE status = 'CONFIRMED';
