package de.evia.travelmate.trips.adapters.web;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.CreateTripCommand;
import de.evia.travelmate.trips.application.representation.TripRepresentation;

@Controller
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;

    public TripController(final TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    public String list(@RequestParam final UUID tenantId, final Model model) {
        model.addAttribute("view", "trip/list");
        model.addAttribute("trips", tripService.findAllByTenantId(new TenantId(tenantId)));
        model.addAttribute("tenantId", tenantId);
        return "layout/default";
    }

    @GetMapping("/new")
    public String form(@RequestParam final UUID tenantId, final Model model) {
        model.addAttribute("view", "trip/form");
        model.addAttribute("tenantId", tenantId);
        return "layout/default";
    }

    @PostMapping
    public String create(@RequestParam final UUID tenantId,
                         @RequestParam final String name,
                         @RequestParam(required = false) final String description,
                         @RequestParam final LocalDate startDate,
                         @RequestParam final LocalDate endDate,
                         @RequestParam final UUID organizerId) {
        final TripRepresentation trip = tripService.createTrip(
            new CreateTripCommand(tenantId, name, description, startDate, endDate, organizerId)
        );
        return "redirect:/trips?tenantId=" + tenantId;
    }
}
