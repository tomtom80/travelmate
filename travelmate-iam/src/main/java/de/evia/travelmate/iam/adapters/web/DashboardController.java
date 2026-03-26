package de.evia.travelmate.iam.adapters.web;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.MessageSource;
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

import jakarta.servlet.http.HttpServletResponse;

import de.evia.travelmate.common.domain.BusinessRuleViolationException;
import de.evia.travelmate.common.domain.DuplicateEntityException;
import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.adapters.mail.RegistrationEmailService;
import de.evia.travelmate.iam.application.AccountService;
import de.evia.travelmate.iam.application.TenantService;
import de.evia.travelmate.iam.application.command.AddDependentCommand;
import de.evia.travelmate.iam.application.command.InviteMemberCommand;
import de.evia.travelmate.iam.application.command.RenameTenantCommand;
import de.evia.travelmate.iam.application.representation.InviteMemberResult;
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
    private final RegistrationEmailService registrationEmailService;
    private final MessageSource messageSource;

    public DashboardController(final AccountRepository accountRepository,
                               final AccountService accountService,
                               final TenantService tenantService,
                               final Optional<RegistrationEmailService> registrationEmailService,
                               final MessageSource messageSource) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.tenantService = tenantService;
        this.registrationEmailService = registrationEmailService.orElse(null);
        this.messageSource = messageSource;
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
                               final HttpServletResponse response,
                               final Locale locale,
                               final Model model) {
        final Account account = resolveAccount(jwt);
        accountService.addDependent(new AddDependentCommand(
            account.tenantId().value(), account.accountId().value(), firstName, lastName, dateOfBirth
        ));
        triggerSuccessToast(response, messageSource.getMessage("companion.added", null, locale));
        model.addAttribute("dependents", accountService.findDependentsByTenantId(account.tenantId()));
        return "dashboard/companions :: companionList";
    }

    @DeleteMapping("/companions/{id}")
    public String deleteCompanion(@AuthenticationPrincipal final Jwt jwt,
                                  @PathVariable final UUID id,
                                  final HttpServletResponse response,
                                  final Locale locale,
                                  final Model model) {
        final Account account = resolveAccount(jwt);
        accountService.deleteDependent(id);
        triggerSuccessToast(response, messageSource.getMessage("companion.deleted", null, locale));
        model.addAttribute("dependents", accountService.findDependentsByTenantId(account.tenantId()));
        return "dashboard/companions :: companionList";
    }

    @PostMapping("/members")
    public String inviteMember(@AuthenticationPrincipal final Jwt jwt,
                               @RequestParam final String email,
                               @RequestParam final String firstName,
                               @RequestParam final String lastName,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate dateOfBirth,
                               final jakarta.servlet.http.HttpServletRequest request,
                               final HttpServletResponse response,
                               final Locale locale,
                               final Model model) {
        final Account account = resolveAccount(jwt);
        try {
            final InviteMemberResult result = accountService.inviteMember(new InviteMemberCommand(
                account.tenantId().value(), email, firstName, lastName, dateOfBirth
            ));
            if (registrationEmailService != null) {
                final String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() != 80 && request.getServerPort() != 443
                    ? ":" + request.getServerPort() : "");
                final String registrationLink = baseUrl + "/iam/register?token=" + result.tokenValue();
                registrationEmailService.sendRegistrationEmail(email, firstName, registrationLink);
            }
            triggerSuccessToast(response, messageSource.getMessage("member.invited", null, locale));
        } catch (final DuplicateEntityException e) {
            model.addAttribute("memberError", e.getMessage());
        }
        model.addAttribute("members", accountService.findAllByTenantId(account.tenantId()));
        model.addAttribute("currentAccount", account);
        return "dashboard/members :: memberList";
    }

    @DeleteMapping("/members/{id}")
    public String deleteMember(@AuthenticationPrincipal final Jwt jwt,
                               @PathVariable final UUID id,
                               final HttpServletResponse response,
                               final Locale locale,
                               final Model model) {
        final Account account = resolveAccount(jwt);
        try {
            accountService.deleteMember(new AccountId(id), account.tenantId());
            triggerSuccessToast(response, messageSource.getMessage("member.deleted", null, locale));
        } catch (final BusinessRuleViolationException e) {
            model.addAttribute("memberError", e.getMessage());
        }
        model.addAttribute("members", accountService.findAllByTenantId(account.tenantId()));
        model.addAttribute("currentAccount", account);
        return "dashboard/members :: memberList";
    }

    @PostMapping("/tenant/rename")
    public String renameTenant(@AuthenticationPrincipal final Jwt jwt,
                               @RequestParam final String name,
                               final HttpServletResponse response,
                               final Locale locale,
                               final Model model) {
        final Account account = resolveAccount(jwt);
        tenantService.renameTenant(new RenameTenantCommand(account.tenantId().value(), name));
        triggerSuccessToast(response, messageSource.getMessage("travelParty.renamed", null, locale));
        model.addAttribute("tenant", tenantService.findById(account.tenantId()));
        return "dashboard/index :: tenantHeader";
    }

    @DeleteMapping("/tenant")
    public String deleteTenant(@AuthenticationPrincipal final Jwt jwt,
                               final HttpServletResponse response) {
        final Account account = resolveAccount(jwt);
        tenantService.deleteTenant(account.tenantId());
        response.setHeader("HX-Redirect", "/logout");
        return "fragments/empty :: empty";
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

    private void triggerSuccessToast(final HttpServletResponse response, final String message) {
        response.setHeader("HX-Trigger",
            "{\"showToast\":{\"level\":\"success\",\"message\":\"" + message.replace("\"", "\\\"") + "\"}}");
    }
}
