package de.evia.travelmate.iam.adapters.web;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.application.AccountService;
import de.evia.travelmate.iam.application.TenantService;
import de.evia.travelmate.iam.application.command.RegisterAccountCommand;
import de.evia.travelmate.iam.application.representation.AccountRepresentation;
import de.evia.travelmate.iam.domain.account.AccountId;

@Controller
@RequestMapping("/tenants/{tenantId}/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TenantService tenantService;

    public AccountController(final AccountService accountService, final TenantService tenantService) {
        this.accountService = accountService;
        this.tenantService = tenantService;
    }

    @GetMapping
    public String list(@PathVariable final UUID tenantId, final Model model) {
        final TenantId tid = new TenantId(tenantId);
        model.addAttribute("title", "Accounts");
        model.addAttribute("tenant", tenantService.findById(tid));
        model.addAttribute("accounts", accountService.findAllByTenantId(tid));
        model.addAttribute("view", "account/list");
        return "layout/default";
    }

    @GetMapping("/new")
    public String form(@PathVariable final UUID tenantId, final Model model) {
        model.addAttribute("title", "Neuer Account");
        model.addAttribute("tenantId", tenantId);
        model.addAttribute("view", "account/form");
        return "layout/default";
    }

    @PostMapping
    public String register(@PathVariable final UUID tenantId,
                           @RequestParam final String keycloakUserId,
                           @RequestParam final String username,
                           @RequestParam final String email,
                           @RequestParam final String firstName,
                           @RequestParam final String lastName,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate dateOfBirth) {
        final AccountRepresentation account = accountService.registerAccount(
            new RegisterAccountCommand(tenantId, keycloakUserId, username, email, firstName, lastName, dateOfBirth));
        return "redirect:/tenants/" + tenantId + "/accounts/" + account.accountId();
    }

    @GetMapping("/{accountId}")
    public String detail(@PathVariable final UUID tenantId,
                         @PathVariable final UUID accountId,
                         final Model model) {
        final AccountRepresentation account = accountService.findById(new AccountId(accountId));
        model.addAttribute("title", account.firstName() + " " + account.lastName());
        model.addAttribute("tenant", tenantService.findById(new TenantId(tenantId)));
        model.addAttribute("account", account);
        model.addAttribute("dependents", accountService.findDependentsByGuardian(new AccountId(accountId)));
        model.addAttribute("view", "account/detail");
        return "layout/default";
    }
}
