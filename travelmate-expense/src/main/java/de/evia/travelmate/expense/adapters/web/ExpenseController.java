package de.evia.travelmate.expense.adapters.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletResponse;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.expense.application.ExpenseService;
import de.evia.travelmate.expense.application.SettlementPdfService;
import de.evia.travelmate.expense.domain.expense.AdvancePaymentSuggestion;
import de.evia.travelmate.expense.domain.expense.CategoryGuesser;
import de.evia.travelmate.expense.domain.expense.ExpenseCategory;
import de.evia.travelmate.expense.domain.expense.ReceiptScanPort;
import de.evia.travelmate.expense.domain.expense.ReceiptScanResult;
import de.evia.travelmate.expense.application.command.AddReceiptCommand;
import de.evia.travelmate.expense.application.command.ApproveReceiptCommand;
import de.evia.travelmate.expense.application.command.ConfirmAdvancePaymentsCommand;
import de.evia.travelmate.expense.application.command.RejectReceiptCommand;
import de.evia.travelmate.expense.application.command.ResubmitReceiptCommand;
import de.evia.travelmate.expense.application.command.ToggleAdvancePaymentPaidCommand;
import de.evia.travelmate.expense.application.command.UpdateWeightingCommand;
import de.evia.travelmate.expense.application.representation.ExpenseRepresentation;
import de.evia.travelmate.expense.domain.trip.TripParticipant;
import de.evia.travelmate.expense.domain.trip.TripProjection;
import de.evia.travelmate.expense.domain.trip.TripProjectionRepository;

@Controller
public class ExpenseController {

    private static final long MAX_UPLOAD_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
        "image/jpeg", "image/png", "image/heic", "image/heif"
    );

    private final ExpenseService expenseService;
    private final TripProjectionRepository tripProjectionRepository;
    private final MessageSource messageSource;
    private final ReceiptScanPort receiptScanPort;
    private final SettlementPdfService settlementPdfService;

    public ExpenseController(final ExpenseService expenseService,
                             final TripProjectionRepository tripProjectionRepository,
                             final MessageSource messageSource,
                             final ReceiptScanPort receiptScanPort,
                             final SettlementPdfService settlementPdfService) {
        this.expenseService = expenseService;
        this.tripProjectionRepository = tripProjectionRepository;
        this.messageSource = messageSource;
        this.receiptScanPort = receiptScanPort;
        this.settlementPdfService = settlementPdfService;
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
        final BigDecimal advancePaidTotal = expense.advancePayments().stream()
            .filter(ap -> ap.paid())
            .map(ap -> ap.amount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        final BigDecimal advanceTotalExpected = expense.advancePayments().stream()
            .map(ap -> ap.amount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        final BigDecimal accommodationPrice = projection.accommodationTotalPrice();
        final long partyCount = countDistinctParties(projection);
        BigDecimal suggestedAdvance = null;
        if (accommodationPrice != null && accommodationPrice.compareTo(BigDecimal.ZERO) > 0
                && partyCount > 0) {
            suggestedAdvance = AdvancePaymentSuggestion.suggest(accommodationPrice, (int) partyCount);
        }

        model.addAttribute("title", messageSource.getMessage("expense.title", null, Locale.GERMAN));
        model.addAttribute("view", "expense/detail");
        model.addAttribute("expense", expense);
        model.addAttribute("tripName", projection.tripName());
        model.addAttribute("tripId", tripId);
        model.addAttribute("participantNames", participantNames);
        model.addAttribute("accommodationPrice", accommodationPrice);
        model.addAttribute("partyCount", partyCount);
        model.addAttribute("suggestedAdvance", suggestedAdvance);
        model.addAttribute("advancePaidTotal", advancePaidTotal);
        model.addAttribute("advanceTotalExpected", advanceTotalExpected);
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

    @PostMapping("/{tripId}/advance-payments")
    public String confirmAdvancePayments(@AuthenticationPrincipal final Jwt jwt,
                                         @PathVariable final UUID tripId,
                                         @RequestParam final BigDecimal amount,
                                         final HttpServletResponse response,
                                         final Locale locale) {
        requireAuthentication(jwt);
        final TripProjection projection = findProjection(tripId);
        final TenantId tenantId = projection.tenantId();

        expenseService.confirmAdvancePayments(
            tenantId, new ConfirmAdvancePaymentsCommand(tripId, amount));

        triggerSuccessToast(response, messageSource.getMessage("advance.confirmed", null, locale));
        return "redirect:/" + tripId;
    }

    @DeleteMapping("/{tripId}/advance-payments")
    public String removeAdvancePayments(@AuthenticationPrincipal final Jwt jwt,
                                        @PathVariable final UUID tripId,
                                        final HttpServletResponse response,
                                        final Locale locale) {
        requireAuthentication(jwt);
        final TripProjection projection = findProjection(tripId);
        final TenantId tenantId = projection.tenantId();

        expenseService.removeAdvancePayments(tenantId, tripId);

        triggerSuccessToast(response, messageSource.getMessage("advance.removed", null, locale));
        return "redirect:/" + tripId;
    }

    @PostMapping("/{tripId}/advance-payments/{advancePaymentId}/toggle-paid")
    public String toggleAdvancePaymentPaid(@AuthenticationPrincipal final Jwt jwt,
                                           @PathVariable final UUID tripId,
                                           @PathVariable final UUID advancePaymentId,
                                           final HttpServletResponse response,
                                           final Locale locale) {
        requireAuthentication(jwt);
        final TripProjection projection = findProjection(tripId);
        final TenantId tenantId = projection.tenantId();
        final UUID markerId = resolveParticipantId(jwt, projection);

        expenseService.toggleAdvancePaymentPaid(
            tenantId, new ToggleAdvancePaymentPaidCommand(tripId, advancePaymentId, markerId));

        triggerSuccessToast(response, messageSource.getMessage("advance.toggledPaid", null, locale));
        return "redirect:/" + tripId;
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

    @PostMapping("/{tripId}/receipts/scan")
    public String scanReceipt(@AuthenticationPrincipal final Jwt jwt,
                              @PathVariable final UUID tripId,
                              @RequestParam("file") final MultipartFile file,
                              final Locale locale,
                              final Model model) throws IOException {
        requireAuthentication(jwt);
        final TripProjection projection = findProjection(tripId);
        final Map<UUID, String> participantNames = buildParticipantNames(projection);

        if (file.isEmpty()) {
            model.addAttribute("scanError",
                messageSource.getMessage("expense.scan.noFile", null, locale));
            return populateScanFormFragment(tripId, participantNames, null, model);
        }

        final String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            model.addAttribute("scanError",
                messageSource.getMessage("expense.scan.invalidFormat", null, locale));
            return populateScanFormFragment(tripId, participantNames, null, model);
        }

        if (file.getSize() > MAX_UPLOAD_SIZE) {
            model.addAttribute("scanError",
                messageSource.getMessage("expense.scan.tooLarge", null, locale));
            return populateScanFormFragment(tripId, participantNames, null, model);
        }

        final byte[] imageData = file.getBytes();
        final ReceiptScanResult scanResult = receiptScanPort.scan(imageData, contentType);

        if (!scanResult.success()) {
            model.addAttribute("scanHint",
                messageSource.getMessage("expense.scan.ocrFailed", null, locale));
        } else {
            if (scanResult.totalAmount() == null) {
                model.addAttribute("scanHint",
                    messageSource.getMessage("expense.scan.amountNotFound", null, locale));
            }
            if (scanResult.receiptDate() == null) {
                model.addAttribute("scanHint",
                    messageSource.getMessage("expense.scan.dateNotFound", null, locale));
            }
        }

        return populateScanFormFragment(tripId, participantNames, scanResult, model);
    }

    @GetMapping(value = "/{tripId}/settlement.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> exportSettlementPdf(
            @AuthenticationPrincipal final Jwt jwt,
            @PathVariable final UUID tripId,
            final Locale locale) {
        requireAuthentication(jwt);
        final TripProjection projection = findProjection(tripId);
        final TenantId tenantId = projection.tenantId();
        final ExpenseRepresentation expense = expenseService.findByTripId(tenantId, tripId, true);

        final byte[] pdfBytes = settlementPdfService.generatePdf(expense, projection, buildParticipantNames(projection), locale);

        final String safeTripName = projection.tripName().replaceAll("[^a-zA-Z0-9äöüÄÖÜß\\-_ ]", "");
        final String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        final String filename = "Abrechnung_" + safeTripName + "_" + dateStr + ".pdf";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfBytes);
    }

    private String populateScanFormFragment(final UUID tripId,
                                             final Map<UUID, String> participantNames,
                                             final ReceiptScanResult scanResult,
                                             final Model model) {
        model.addAttribute("tripId", tripId);
        model.addAttribute("participantNames", participantNames);
        model.addAttribute("scanResult", scanResult);
        if (scanResult != null) {
            model.addAttribute("prefillAmount", scanResult.totalAmount());
            model.addAttribute("prefillDate", scanResult.receiptDate() != null
                ? scanResult.receiptDate() : LocalDate.now());
            model.addAttribute("prefillDescription", scanResult.storeName() != null
                ? scanResult.storeName() : "");
            final ExpenseCategory guessedCategory = scanResult.suggestedCategory() != null
                ? scanResult.suggestedCategory()
                : CategoryGuesser.guess(scanResult.storeName());
            model.addAttribute("prefillCategory", guessedCategory);
        } else {
            model.addAttribute("prefillDate", LocalDate.now());
            model.addAttribute("prefillDescription", "");
            model.addAttribute("prefillCategory", ExpenseCategory.OTHER);
        }
        model.addAttribute("view", "expense/scan-result");
        return "expense/scan-result :: scanResultForm";
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

    private long countDistinctParties(final TripProjection projection) {
        return projection.participants().stream()
            .filter(TripParticipant::hasPartyInfo)
            .map(TripParticipant::partyTenantId)
            .distinct()
            .count();
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
