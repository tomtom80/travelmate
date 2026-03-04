package de.evia.travelmate.iam.adapters.web;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.application.TenantService;
import de.evia.travelmate.iam.application.command.CreateTenantCommand;
import de.evia.travelmate.iam.application.representation.TenantRepresentation;

@Controller
@RequestMapping("/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(final TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public String list(final Model model) {
        model.addAttribute("title", "Tenants");
        model.addAttribute("tenants", tenantService.findAll());
        model.addAttribute("view", "tenant/list");
        return "layout/default";
    }

    @GetMapping("/new")
    public String form(final Model model) {
        model.addAttribute("title", "Neuer Tenant");
        model.addAttribute("view", "tenant/form");
        return "layout/default";
    }

    @PostMapping
    public String create(@RequestParam final String name,
                         @RequestParam(required = false) final String description) {
        final TenantRepresentation tenant = tenantService.createTenant(
            new CreateTenantCommand(name, description));
        return "redirect:/tenants/" + tenant.tenantId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable final UUID id, final Model model) {
        final TenantRepresentation tenant = tenantService.findById(new TenantId(id));
        model.addAttribute("title", tenant.name());
        model.addAttribute("tenant", tenant);
        model.addAttribute("view", "tenant/detail");
        return "layout/default";
    }
}
