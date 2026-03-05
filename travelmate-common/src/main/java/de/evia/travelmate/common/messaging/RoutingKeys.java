package de.evia.travelmate.common.messaging;

public final class RoutingKeys {

    private RoutingKeys() {
    }

    public static final String EXCHANGE = "travelmate.events";

    public static final String TENANT_CREATED = "iam.tenant-created";
    public static final String ACCOUNT_REGISTERED = "iam.account-registered";
    public static final String MEMBER_ADDED = "iam.member-added";
    public static final String DEPENDENT_ADDED = "iam.dependent-added";
    public static final String MEMBER_REMOVED = "iam.member-removed";
    public static final String DEPENDENT_REMOVED = "iam.dependent-removed";
    public static final String TENANT_DELETED = "iam.tenant-deleted";

    public static final String TRIP_CREATED = "trips.trip-created";
    public static final String PARTICIPANT_CONFIRMED = "trips.participant-confirmed";
    public static final String TRIP_COMPLETED = "trips.trip-completed";
}
