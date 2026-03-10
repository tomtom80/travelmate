package de.evia.travelmate.iam.adapters.web;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.application.AccountService;
import de.evia.travelmate.iam.application.TenantService;
import de.evia.travelmate.iam.application.command.AddDependentCommand;
import de.evia.travelmate.iam.application.command.InviteMemberCommand;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountId;
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
        final Optional<Account> maybeAccount = findAccount(jwt);
        if (maybeAccount.isEmpty()) {
            return "redirect:/signup";
        }
        final Account account = maybeAccount.get();
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
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate dateOfBirth,
                               final Model model) {
        final Account account = resolveAccount(jwt);
        accountService.addDependent(new AddDependentCommand(
            account.tenantId().value(), account.accountId().value(), firstName, lastName, dateOfBirth
        ));
        model.addAttribute("dependents", accountService.findDependentsByTenantId(account.tenantId()));
        return "dashboard/companions :: companionList";
    }

    @DeleteMapping("/companions/{id}")
    public String deleteCompanion(@AuthenticationPrincipal final Jwt jwt,
                                  @PathVariable final UUID id,
                                  final Model model) {
        final Account account = resolveAccount(jwt);
        accountService.deleteDependent(id);
        model.addAttribute("dependents", accountService.findDependentsByTenantId(account.tenantId()));
        return "dashboard/companions :: companionList";
    }

    @PostMapping("/members")
    public String inviteMember(@AuthenticationPrincipal final Jwt jwt,
                               @RequestParam final String email,
                               @RequestParam final String firstName,
                               @RequestParam final String lastName,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate dateOfBirth,
                               final Model model) {
        final Account account = resolveAccount(jwt);
        try {
            accountService.inviteMember(new InviteMemberCommand(
                account.tenantId().value(), email, firstName, lastName, dateOfBirth
            ));
        } catch (final IllegalArgumentException e) {
            model.addAttribute("memberError", e.getMessage());
        }
        model.addAttribute("members", accountService.findAllByTenantId(account.tenantId()));
        model.addAttribute("currentAccount", account);
        return "dashboard/members :: memberList";
    }

    @DeleteMapping("/members/{id}")
    public String deleteMember(@AuthenticationPrincipal final Jwt jwt,
                               @PathVariable final UUID id,
                               final Model model) {
        final Account account = resolveAccount(jwt);
        try {
            accountService.deleteMember(new AccountId(id), account.tenantId());
        } catch (final IllegalArgumentException e) {
            model.addAttribute("memberError", e.getMessage());
        }
        model.addAttribute("members", accountService.findAllByTenantId(account.tenantId()));
        model.addAttribute("currentAccount", account);
        return "dashboard/members :: memberList";
    }

    @DeleteMapping("/tenant")
    public String deleteTenant(@AuthenticationPrincipal final Jwt jwt) {
        final Account account = resolveAccount(jwt);
        tenantService.deleteTenant(account.tenantId());
        return "redirect:/logout";
    }

    private Optional<Account> findAccount(final Jwt jwt) {
        final String keycloakUserId = jwt.getSubject();
        return accountRepository.findByKeycloakUserId(new KeycloakUserId(keycloakUserId));
    }

    private Account resolveAccount(final Jwt jwt) {
        return findAccount(jwt)
            .orElseThrow(() -> new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Account not found"));
    }
}
