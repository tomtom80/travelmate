-- TravelParty: local projection of IAM data consumed via events

CREATE TABLE travel_party (
    tenant_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE travel_party_member (
    member_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES travel_party(tenant_id),
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL
);

CREATE TABLE travel_party_dependent (
    dependent_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES travel_party(tenant_id),
    guardian_member_id UUID NOT NULL REFERENCES travel_party_member(member_id),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL
);
