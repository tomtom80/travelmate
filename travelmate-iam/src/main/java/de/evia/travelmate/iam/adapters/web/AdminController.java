package de.evia.travelmate.iam.adapters.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.application.TenantService;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final TenantService tenantService;

    public AdminController(final TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @DeleteMapping("/tenants/{tenantId}")
    public ResponseEntity<Void> deleteTenant(@PathVariable final UUID tenantId) {
        tenantService.deleteTenant(new TenantId(tenantId));
        return ResponseEntity.noContent().build();
    }
}
