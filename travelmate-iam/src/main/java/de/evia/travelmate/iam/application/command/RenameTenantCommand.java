package de.evia.travelmate.iam.application.command;

import java.util.UUID;

public record RenameTenantCommand(
    UUID tenantId,
    String newName
) {
}
