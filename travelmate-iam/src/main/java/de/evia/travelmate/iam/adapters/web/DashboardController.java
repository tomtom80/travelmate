package de.evia.travelmate.iam.adapters.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.application.AccountService;
import de.evia.travelmate.iam.application.TenantService;
import de.evia.travelmate.iam.application.command.AddDependentCommand;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final TenantService tenantService;

    public DashboardController(final AccountRepository accountRepository,
                               final AccountService accountService,
                               final TenantService tenantService) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.tenantService = tenantService;
    }

    @GetMapping
    public String dashboard(@AuthenticationPrincipal final Jwt jwt, final Model model) {
        final Account account = resolveAccount(jwt);
        final TenantId tenantId = account.tenantId();

        model.addAttribute("view", "dashboard/index");
        model.addAttribute("tenant", tenantService.findById(tenantId));
        model.addAttribute("members", accountService.findAllByTenantId(tenantId));
        model.addAttribute("dependents", accountService.findDependentsByTenantId(tenantId));
        model.addAttribute("currentAccount", account);
        return "layout/default";
    }

    @PostMapping("/companions")
    public String addCompanion(@AuthenticationPrincipal final Jwt jwt,
                               @RequestParam final String firstName,
                               @RequestParam final String lastName,
                               final Model model) {
        final Account account = resolveAccount(jwt);
        accountService.addDependent(new AddDependentCommand(
            account.tenantId().value(), account.accountId().value(), firstName, lastName
        ));
        model.addAttribute("dependents", accountService.findDependentsByTenantId(account.tenantId()));
        return "dashboard/companions :: companionList";
    }

    private Account resolveAccount(final Jwt jwt) {
        final String keycloakUserId = jwt.getSubject();
        return accountRepository.findByKeycloakUserId(new KeycloakUserId(keycloakUserId))
            .orElseThrow(() -> new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Account not found"));
    }
}
