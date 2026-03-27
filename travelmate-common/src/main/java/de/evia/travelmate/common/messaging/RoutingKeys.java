package de.evia.travelmate.common.messaging;

public final class RoutingKeys {

    private RoutingKeys() {
    }

    public static final String EXCHANGE = "travelmate.events";
    public static final String DLX_EXCHANGE = "travelmate.events.dlx";

    public static final String TENANT_CREATED = "iam.tenant-created";
    public static final String ACCOUNT_REGISTERED = "iam.account-registered";
    public static final String MEMBER_ADDED = "iam.member-added";
    public static final String DEPENDENT_ADDED = "iam.dependent-added";
    public static final String MEMBER_REMOVED = "iam.member-removed";
    public static final String DEPENDENT_REMOVED = "iam.dependent-removed";
    public static final String TENANT_RENAMED = "iam.tenant-renamed";
    public static final String TENANT_DELETED = "iam.tenant-deleted";

    public static final String TRIP_CREATED = "trips.trip-created";
    public static final String INVITATION_CREATED = "trips.invitation-created";
    public static final String EXTERNAL_USER_INVITED = "trips.external-user-invited";
    public static final String PARTICIPANT_CONFIRMED = "trips.participant-confirmed";
    public static final String PARTICIPANT_REMOVED = "trips.participant-removed";
    public static final String STAY_PERIOD_UPDATED = "trips.stay-period-updated";
    public static final String TRIP_COMPLETED = "trips.trip-completed";
    public static final String ACCOMMODATION_PRICE_SET = "trips.accommodation.price-set";
}
