package de.evia.travelmate.expense.adapters.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletResponse;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.expense.application.ExpenseService;
import de.evia.travelmate.expense.domain.expense.ExpenseCategory;
import de.evia.travelmate.expense.application.command.AddReceiptCommand;
import de.evia.travelmate.expense.application.command.ApproveReceiptCommand;
import de.evia.travelmate.expense.application.command.RejectReceiptCommand;
import de.evia.travelmate.expense.application.command.ResubmitReceiptCommand;
import de.evia.travelmate.expense.application.command.UpdateWeightingCommand;
import de.evia.travelmate.expense.application.representation.ExpenseRepresentation;
import de.evia.travelmate.expense.domain.trip.TripParticipant;
import de.evia.travelmate.expense.domain.trip.TripProjection;
import de.evia.travelmate.expense.domain.trip.TripProjectionRepository;

@Controller
public class ExpenseController {

    private final ExpenseService expenseService;
    private final TripProjectionRepository tripProjectionRepository;
    private final MessageSource messageSource;

    public ExpenseController(final ExpenseService expenseService,
                             final TripProjectionRepository tripProjectionRepository,
                             final MessageSource messageSource) {
        this.expenseService = expenseService;
        this.tripProjectionRepository = tripProjectionRepository;
        this.messageSource = messageSource;
    }

    @GetMapping("/")
    public String home(final Model model) {
        model.addAttribute("view", "index");
        return "layout/default";
    }

    @GetMapping("/{tripId}")
    public String detail(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         final Model model) {
        requireAuthentication(jwt);
        final TripProjection projection = findProjection(tripId);
        final TenantId tenantId = projection.tenantId();
        final Map<UUID, String> participantNames = buildParticipantNames(projection);

        final ExpenseRepresentation expense = expenseService.findByTripId(tenantId, tripId, true);

        model.addAttribute("title", messageSource.getMessage("expense.title", null, Locale.GERMAN));
        model.addAttribute("view", "expense/detail");
        model.addAttribute("expense", expense);
        model.addAttribute("tripName", projection.tripName());
        model.addAttribute("tripId", tripId);
        model.addAttribute("participantNames", participantNames);
        return "layout/default";
    }

    @PostMapping("/{tripId}/receipts")
    public String addReceipt(@AuthenticationPrincipal final Jwt jwt,
                             @PathVariable final UUID tripId,
                             @RequestParam final String description,
                             @RequestParam final BigDecimal amount,
                             @RequestParam final UUID paidBy,
                             @RequestParam final LocalDate date,
                             @RequestParam(required = false) final ExpenseCategory category,
                             final HttpServletResponse response,
                             final Locale locale,
                             final Model model) {
        requireAuthentication(jwt);
        final TripProjection projection = findProjection(tripId);
        final TenantId tenantId = projection.tenantId();

        final ExpenseRepresentation expense = expenseService.addReceipt(
            tenantId, new AddReceiptCommand(tripId, description, amount, paidBy, paidBy, date, category));

        triggerSuccessToast(response, messageSource.getMessage("expense.receiptAdded", null, locale));
        return populateReceiptFragment(expense, projection, model);
    }

    @PostMapping("/{tripId}/receipts/{receiptId}/approve")
    public String approveReceipt(@AuthenticationPrincipal final Jwt jwt,
                                 @PathVariable final UUID tripId,
                                 @PathVariable final UUID receiptId,
                                 final HttpServletResponse response,
                                 final Locale locale,
                                 final Model model) {
        requireAuthentication(jwt);
        final TripProjection projection = findProjection(tripId);
        final TenantId tenantId = projection.tenantId();
        final UUID reviewerId = resolveParticipantId(jwt, projection);

        final ExpenseRepresentation expense = expenseService.approveReceipt(
            tenantId, new ApproveReceiptCommand(tripId, receiptId, reviewerId));

        triggerSuccessToast(response, messageSource.getMessage("expense.receiptApproved", null, locale));
        return populateReceiptFragment(expense, projection, model);
    }

    @PostMapping("/{tripId}/receipts/{receiptId}/reject")
    public String rejectReceipt(@AuthenticationPrincipal final Jwt jwt,
                                @PathVariable final UUID tripId,
                                @PathVariable final UUID receiptId,
                                @RequestParam final String reason,
                                final HttpServletResponse response,
                                final Locale locale,
                                final Model model) {
        requireAuthentication(jwt);
        final TripProjection projection = findProjection(tripId);
        final TenantId tenantId = projection.tenantId();
        final UUID reviewerId = resolveParticipantId(jwt, projection);

        final ExpenseRepresentation expense = expenseService.rejectReceipt(
            tenantId, new RejectReceiptCommand(tripId, receiptId, reviewerId, reason));

        triggerSuccessToast(response, messageSource.getMessage("expense.receiptRejected", null, locale));
        return populateReceiptFragment(expense, projection, model);
    }

    @PostMapping("/{tripId}/receipts/{receiptId}/resubmit")
    public String resubmitReceipt(@AuthenticationPrincipal final Jwt jwt,
                                  @PathVariable final UUID tripId,
                                  @PathVariable final UUID receiptId,
                                  @RequestParam final String description,
                                  @RequestParam final BigDecimal amount,
                                  @RequestParam final LocalDate date,
                                  @RequestParam(required = false) final ExpenseCategory category,
                                  final HttpServletResponse response,
                                  final Locale locale,
                                  final Model model) {
        requireAuthentication(jwt);
        final TripProjection projection = findProjection(tripId);
        final TenantId tenantId = projection.tenantId();

        final ExpenseRepresentation expense = expenseService.resubmitReceipt(
            tenantId, new ResubmitReceiptCommand(tripId, receiptId, description, amount, date, category));

        triggerSuccessToast(response, messageSource.getMessage("expense.receiptResubmitted", null, locale));
        return populateReceiptFragment(expense, projection, model);
    }

    @DeleteMapping("/{tripId}/receipts/{receiptId}")
    public String removeReceipt(@AuthenticationPrincipal final Jwt jwt,
                                @PathVariable final UUID tripId,
                                @PathVariable final UUID receiptId,
                                final HttpServletResponse response,
                                final Locale locale,
                                final Model model) {
        requireAuthentication(jwt);
        final TripProjection projection = findProjection(tripId);
        final TenantId tenantId = projection.tenantId();

        final ExpenseRepresentation expense = expenseService.removeReceipt(tenantId, tripId, receiptId);

        triggerSuccessToast(response, messageSource.getMessage("expense.receiptRemoved", null, locale));
        return populateReceiptFragment(expense, projection, model);
    }

    @PostMapping("/{tripId}/weightings")
    public String updateWeighting(@AuthenticationPrincipal final Jwt jwt,
                                  @PathVariable final UUID tripId,
                                  @RequestParam final UUID participantId,
                                  @RequestParam final BigDecimal weight,
                                  final HttpServletResponse response,
                                  final Locale locale,
                                  final Model model) {
        requireAuthentication(jwt);
        final TripProjection projection = findProjection(tripId);
        final TenantId tenantId = projection.tenantId();

        final ExpenseRepresentation expense = expenseService.updateWeighting(
            tenantId, new UpdateWeightingCommand(tripId, participantId, weight));

        triggerSuccessToast(response, messageSource.getMessage("expense.weightingUpdated", null, locale));
        return populateWeightingFragment(expense, projection, model);
    }

    @PostMapping("/{tripId}/settle")
    public String settle(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId) {
        requireAuthentication(jwt);
        final TripProjection projection = findProjection(tripId);
        final TenantId tenantId = projection.tenantId();

        expenseService.settle(tenantId, tripId);
        return "redirect:/" + tripId;
    }

    private void requireAuthentication(final Jwt jwt) {
        if (jwt == null || jwt.getClaimAsString("email") == null) {
            throw new ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Authentication required");
        }
    }

    private TripProjection findProjection(final UUID tripId) {
        return tripProjectionRepository.findByTripId(tripId)
            .orElseThrow(() -> new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Trip not found"));
    }

    private UUID resolveParticipantId(final Jwt jwt, final TripProjection projection) {
        final String email = jwt.getClaimAsString("email");
        return projection.participants().stream()
            .filter(p -> p.name().equalsIgnoreCase(email))
            .map(TripParticipant::participantId)
            .findFirst()
            .orElse(projection.participants().getFirst().participantId());
    }

    private Map<UUID, String> buildParticipantNames(final TripProjection projection) {
        final Map<UUID, String> names = new LinkedHashMap<>();
        for (final TripParticipant participant : projection.participants()) {
            names.put(participant.participantId(), participant.name());
        }
        return names;
    }

    private String populateReceiptFragment(final ExpenseRepresentation expense,
                                           final TripProjection projection,
                                           final Model model) {
        model.addAttribute("expense", expense);
        model.addAttribute("participantNames", buildParticipantNames(projection));
        model.addAttribute("tripId", projection.tripId());
        return "expense/receipts :: receiptList";
    }

    private String populateWeightingFragment(final ExpenseRepresentation expense,
                                             final TripProjection projection,
                                             final Model model) {
        model.addAttribute("expense", expense);
        model.addAttribute("participantNames", buildParticipantNames(projection));
        model.addAttribute("tripId", projection.tripId());
        return "expense/weightings :: weightingList";
    }

    private void triggerSuccessToast(final HttpServletResponse response, final String message) {
        response.setHeader("HX-Trigger",
            "{\"showToast\":{\"level\":\"success\",\"message\":\"" + message.replace("\"", "\\\"") + "\"}}");
    }
}
