package de.evia.travelmate.trips.domain.accommodation;

import java.util.Optional;

public interface AccommodationImportPort {

    Optional<AccommodationImportResult> importFromUrl(String url);
}
