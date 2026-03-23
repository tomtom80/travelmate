package de.evia.travelmate.expense.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.expense.application.ExpenseService;
import de.evia.travelmate.expense.application.SettlementPdfService;
import de.evia.travelmate.expense.application.command.AddReceiptCommand;
import de.evia.travelmate.expense.application.command.ApproveReceiptCommand;
import de.evia.travelmate.expense.application.command.ConfirmAdvancePaymentsCommand;
import de.evia.travelmate.expense.application.command.RejectReceiptCommand;
import de.evia.travelmate.expense.application.command.ResubmitReceiptCommand;
import de.evia.travelmate.expense.application.command.ToggleAdvancePaymentPaidCommand;
import de.evia.travelmate.expense.application.command.UpdateWeightingCommand;
import de.evia.travelmate.expense.application.representation.AdvancePaymentRepresentation;
import de.evia.travelmate.expense.application.representation.ExpenseRepresentation;
import de.evia.travelmate.expense.application.representation.ReceiptRepresentation;
import de.evia.travelmate.expense.application.representation.CategoryBreakdownRepresentation;
import de.evia.travelmate.expense.application.representation.DailyCostRepresentation;
import de.evia.travelmate.expense.application.representation.ParticipantSummaryRepresentation;
import de.evia.travelmate.expense.application.representation.PartyAccountEntryRepresentation;
import de.evia.travelmate.expense.application.representation.PartyAccountRepresentation;
import de.evia.travelmate.expense.application.representation.TransferRepresentation;
import de.evia.travelmate.expense.application.representation.WeightingRepresentation;
import de.evia.travelmate.expense.domain.expense.PartyAccountEntryType;
import de.evia.travelmate.expense.domain.expense.ExpenseStatus;
import de.evia.travelmate.expense.domain.expense.ReviewStatus;
import de.evia.travelmate.expense.domain.trip.TripParticipant;
import de.evia.travelmate.expense.domain.trip.TripProjection;
import de.evia.travelmate.expense.domain.trip.TripProjectionRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpenseControllerTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID TRIP_UUID = UUID.randomUUID();
    private static final UUID EXPENSE_UUID = UUID.randomUUID();
    private static final UUID PARTICIPANT_A = UUID.randomUUID();
    private static final UUID PARTICIPANT_B = UUID.randomUUID();
    private static final UUID RECEIPT_UUID = UUID.randomUUID();
    private static final String USER_EMAIL = "user@test.de";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService expenseService;

    @MockitoBean
    private TripProjectionRepository tripProjectionRepository;

    @MockitoBean
    private SettlementPdfService settlementPdfService;

    private TripProjection projection;
    private ExpenseRepresentation expense;

    @BeforeEach
    void setUp() {
        projection = new TripProjection(
            TRIP_UUID,
            new TenantId(TENANT_UUID),
            "Skiurlaub",
            List.of(
                new TripParticipant(PARTICIPANT_A, "Max Mustermann"),
                new TripParticipant(PARTICIPANT_B, "Lisa Muster")
            )
        );
        when(tripProjectionRepository.findByTripId(TRIP_UUID)).thenReturn(Optional.of(projection));

        expense = new ExpenseRepresentation(
            EXPENSE_UUID,
            TRIP_UUID,
            ExpenseStatus.OPEN,
            false,
            List.of(new ReceiptRepresentation(
                RECEIPT_UUID, "Supermarkt", new BigDecimal("42.50"), PARTICIPANT_A, PARTICIPANT_A,
                LocalDate.of(2026, 3, 10), null, ReviewStatus.APPROVED, null, null
            )),
            List.of(
                new WeightingRepresentation(PARTICIPANT_A, BigDecimal.ONE, BigDecimal.ONE, 34, "ADULT"),
                new WeightingRepresentation(PARTICIPANT_B, BigDecimal.ONE, BigDecimal.ONE, 33, "ADULT")
            ),
            Map.of(PARTICIPANT_A, new BigDecimal("21.25"), PARTICIPANT_B, new BigDecimal("-21.25")),
            List.of(new TransferRepresentation(PARTICIPANT_B, PARTICIPANT_A, new BigDecimal("21.25"))),
            List.of(new CategoryBreakdownRepresentation(null, new BigDecimal("42.50"), new BigDecimal("100.0"), 1)),
            List.of(
                new ParticipantSummaryRepresentation(PARTICIPANT_A, new BigDecimal("42.50"), new BigDecimal("21.25"), new BigDecimal("21.25")),
                new ParticipantSummaryRepresentation(PARTICIPANT_B, BigDecimal.ZERO, new BigDecimal("21.25"), new BigDecimal("-21.25"))
            ),
            List.of(new DailyCostRepresentation(LocalDate.of(2026, 3, 10), new BigDecimal("42.50"), 1)),
            new BigDecimal("42.50"),
            List.of(
                new PartyAccountRepresentation(UUID.randomUUID(), "Familie Mustermann", List.of("Max Mustermann"),
                    List.of(new PartyAccountEntryRepresentation(PartyAccountEntryType.RECEIPT_CREDIT, "Receipt credit", new BigDecimal("42.50"), new BigDecimal("42.50"))),
                    new BigDecimal("42.50"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("21.25"), new BigDecimal("21.25"), BigDecimal.ZERO, new BigDecimal("21.25"))
            ),
            List.of(),
            List.of(),
            List.of()
        );
    }

    @Test
    void detailShowsExpenseForTrip() throws Exception {
        when(expenseService.findByTripId(new TenantId(TENANT_UUID), TRIP_UUID, true))
            .thenReturn(expense);

        mockMvc.perform(get("/" + TRIP_UUID)
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "expense/detail"))
            .andExpect(model().attributeExists("expense"))
            .andExpect(model().attributeExists("tripName"))
            .andExpect(model().attributeExists("participantNames"))
            .andExpect(model().attribute("tripName", "Skiurlaub"));
    }

    @Test
    void detailReturns404WhenTripNotFound() throws Exception {
        final UUID unknownTrip = UUID.randomUUID();
        when(tripProjectionRepository.findByTripId(unknownTrip)).thenReturn(Optional.empty());

        mockMvc.perform(get("/" + unknownTrip)
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL))))
            .andExpect(status().isNotFound());
    }

    @Test
    void addReceiptReturnsReceiptFragment() throws Exception {
        when(expenseService.addReceipt(eq(new TenantId(TENANT_UUID)), any(AddReceiptCommand.class)))
            .thenReturn(expense);

        mockMvc.perform(post("/" + TRIP_UUID + "/receipts")
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL)))
                .param("description", "Supermarkt")
                .param("amount", "42.50")
                .param("paidBy", PARTICIPANT_A.toString())
                .param("date", "2026-03-10")
                .param("category", "GROCERIES"))
            .andExpect(status().isOk())
            .andExpect(view().name("expense/receipts :: receiptList"))
            .andExpect(model().attributeExists("expense"))
            .andExpect(model().attributeExists("participantNames"))
            .andExpect(header().exists("HX-Trigger"));

        verify(expenseService).addReceipt(eq(new TenantId(TENANT_UUID)), any(AddReceiptCommand.class));
    }

    @Test
    void removeReceiptReturnsReceiptFragment() throws Exception {
        when(expenseService.removeReceipt(new TenantId(TENANT_UUID), TRIP_UUID, RECEIPT_UUID))
            .thenReturn(expense);

        mockMvc.perform(delete("/" + TRIP_UUID + "/receipts/" + RECEIPT_UUID)
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("expense/receipts :: receiptList"))
            .andExpect(model().attributeExists("expense"))
            .andExpect(header().exists("HX-Trigger"));

        verify(expenseService).removeReceipt(new TenantId(TENANT_UUID), TRIP_UUID, RECEIPT_UUID);
    }

    @Test
    void updateWeightingReturnsWeightingFragment() throws Exception {
        when(expenseService.updateWeighting(eq(new TenantId(TENANT_UUID)), any(UpdateWeightingCommand.class)))
            .thenReturn(expense);

        mockMvc.perform(post("/" + TRIP_UUID + "/weightings")
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL)))
                .param("participantId", PARTICIPANT_A.toString())
                .param("weight", "0.5"))
            .andExpect(status().isOk())
            .andExpect(view().name("expense/weightings :: weightingList"))
            .andExpect(header().exists("HX-Trigger"));

        verify(expenseService).updateWeighting(eq(new TenantId(TENANT_UUID)), any(UpdateWeightingCommand.class));
    }

    @Test
    void settleRedirectsToDetail() throws Exception {
        when(expenseService.settle(new TenantId(TENANT_UUID), TRIP_UUID))
            .thenReturn(expense);

        mockMvc.perform(post("/" + TRIP_UUID + "/settle")
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID));

        verify(expenseService).settle(new TenantId(TENANT_UUID), TRIP_UUID);
    }

    @Test
    void approveReceiptReturnsReceiptFragment() throws Exception {
        when(expenseService.approveReceipt(eq(new TenantId(TENANT_UUID)), any(ApproveReceiptCommand.class)))
            .thenReturn(expense);

        mockMvc.perform(post("/" + TRIP_UUID + "/receipts/" + RECEIPT_UUID + "/approve")
                .with(jwt().jwt(j -> j.claim("email", "Max Mustermann"))))
            .andExpect(status().isOk())
            .andExpect(view().name("expense/receipts :: receiptList"))
            .andExpect(model().attributeExists("expense"))
            .andExpect(model().attributeExists("participantNames"))
            .andExpect(header().exists("HX-Trigger"));

        verify(expenseService).approveReceipt(eq(new TenantId(TENANT_UUID)), any(ApproveReceiptCommand.class));
    }

    @Test
    void rejectReceiptReturnsReceiptFragment() throws Exception {
        when(expenseService.rejectReceipt(eq(new TenantId(TENANT_UUID)), any(RejectReceiptCommand.class)))
            .thenReturn(expense);

        mockMvc.perform(post("/" + TRIP_UUID + "/receipts/" + RECEIPT_UUID + "/reject")
                .with(jwt().jwt(j -> j.claim("email", "Max Mustermann")))
                .param("reason", "Wrong amount"))
            .andExpect(status().isOk())
            .andExpect(view().name("expense/receipts :: receiptList"))
            .andExpect(model().attributeExists("expense"))
            .andExpect(model().attributeExists("participantNames"))
            .andExpect(header().exists("HX-Trigger"));

        verify(expenseService).rejectReceipt(eq(new TenantId(TENANT_UUID)), any(RejectReceiptCommand.class));
    }

    @Test
    void resubmitReceiptReturnsReceiptFragment() throws Exception {
        when(expenseService.resubmitReceipt(eq(new TenantId(TENANT_UUID)), any(ResubmitReceiptCommand.class)))
            .thenReturn(expense);

        mockMvc.perform(post("/" + TRIP_UUID + "/receipts/" + RECEIPT_UUID + "/resubmit")
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL)))
                .param("description", "Corrected Supermarkt")
                .param("amount", "45.00")
                .param("date", "2026-03-10")
                .param("category", "GROCERIES"))
            .andExpect(status().isOk())
            .andExpect(view().name("expense/receipts :: receiptList"))
            .andExpect(model().attributeExists("expense"))
            .andExpect(model().attributeExists("participantNames"))
            .andExpect(header().exists("HX-Trigger"));

        verify(expenseService).resubmitReceipt(eq(new TenantId(TENANT_UUID)), any(ResubmitReceiptCommand.class));
    }

    @Test
    void confirmAdvancePaymentsRedirectsToDetail() throws Exception {
        when(expenseService.confirmAdvancePayments(eq(new TenantId(TENANT_UUID)),
            any(ConfirmAdvancePaymentsCommand.class))).thenReturn(expense);

        mockMvc.perform(post("/" + TRIP_UUID + "/advance-payments")
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL)))
                .param("amount", "500.00"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID));

        verify(expenseService).confirmAdvancePayments(eq(new TenantId(TENANT_UUID)),
            any(ConfirmAdvancePaymentsCommand.class));
    }

    @Test
    void removeAdvancePaymentsRedirectsToDetail() throws Exception {
        when(expenseService.removeAdvancePayments(new TenantId(TENANT_UUID), TRIP_UUID))
            .thenReturn(expense);

        mockMvc.perform(delete("/" + TRIP_UUID + "/advance-payments")
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID));

        verify(expenseService).removeAdvancePayments(new TenantId(TENANT_UUID), TRIP_UUID);
    }

    @Test
    void toggleAdvancePaymentPaidRedirectsToDetail() throws Exception {
        final UUID apId = UUID.randomUUID();
        when(expenseService.toggleAdvancePaymentPaid(eq(new TenantId(TENANT_UUID)),
            any(ToggleAdvancePaymentPaidCommand.class))).thenReturn(expense);

        mockMvc.perform(post("/" + TRIP_UUID + "/advance-payments/" + apId + "/toggle-paid")
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/" + TRIP_UUID));

        verify(expenseService).toggleAdvancePaymentPaid(eq(new TenantId(TENANT_UUID)),
            any(ToggleAdvancePaymentPaidCommand.class));
    }

    @Test
    void homePageReturnsOk() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"));
    }

    // --- Receipt Scan (S10-B) ---

    @Test
    void scanReceiptReturnsPrefilledForm() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
            "file", "receipt.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/" + TRIP_UUID + "/receipts/scan")
                .file(file)
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("expense/scan-result :: scanResultForm"))
            .andExpect(model().attributeExists("tripId"))
            .andExpect(model().attributeExists("participantNames"))
            .andExpect(model().attributeExists("scanResult"));
    }

    @Test
    void scanReceiptRejectsInvalidMimeType() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/" + TRIP_UUID + "/receipts/scan")
                .file(file)
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("scanError"));
    }

    @Test
    void scanReceiptRejectsEmptyFile() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
            "file", "empty.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/" + TRIP_UUID + "/receipts/scan")
                .file(file)
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("scanError"));
    }

    // --- Settlement PDF (S10-D) ---

    @Test
    void exportSettlementPdfReturnsPdf() throws Exception {
        when(expenseService.findByTripId(new TenantId(TENANT_UUID), TRIP_UUID, true))
            .thenReturn(expense);
        when(settlementPdfService.generatePdf(any(), any(), any()))
            .thenReturn(new byte[]{37, 80, 68, 70}); // %PDF

        mockMvc.perform(get("/" + TRIP_UUID + "/settlement.pdf")
                .with(jwt().jwt(j -> j.claim("email", USER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().exists("Content-Disposition"));

        verify(settlementPdfService).generatePdf(any(), any(), any());
    }
}
