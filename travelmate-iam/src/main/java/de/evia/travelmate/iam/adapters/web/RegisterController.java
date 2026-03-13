package de.evia.travelmate.iam.adapters.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.iam.application.RegistrationService;
import de.evia.travelmate.iam.application.command.CompleteRegistrationCommand;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.registration.InvitationToken;

@Controller
@RequestMapping("/register")
public class RegisterController {

    private final RegistrationService registrationService;
    private final AccountRepository accountRepository;

    public RegisterController(final RegistrationService registrationService,
                              final AccountRepository accountRepository) {
        this.registrationService = registrationService;
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public String showForm(@RequestParam final String token, final Model model) {
        try {
            final InvitationToken invitationToken = registrationService.findByTokenValue(token);

            if (invitationToken.isExpired()) {
                model.addAttribute("error", "register.error.expired");
                return "register/error";
            }
            if (invitationToken.isUsed()) {
                model.addAttribute("error", "register.error.alreadyUsed");
                return "register/error";
            }

            final Account account = accountRepository.findById(invitationToken.accountId())
                .orElseThrow(() -> new EntityNotFoundException("Account", invitationToken.accountId().value().toString()));

            model.addAttribute("token", token);
            model.addAttribute("firstName", account.fullName().firstName());
            model.addAttribute("lastName", account.fullName().lastName());
            model.addAttribute("email", account.email().value());
            return "register/form";
        } catch (final EntityNotFoundException e) {
            model.addAttribute("error", "register.error.invalidToken");
            return "register/error";
        }
    }

    @PostMapping
    public String completeRegistration(@RequestParam final String token,
                                       @RequestParam final String password,
                                       final Model model) {
        try {
            registrationService.completeRegistration(new CompleteRegistrationCommand(token, password));
            return "register/success";
        } catch (final IllegalStateException e) {
            model.addAttribute("error", e.getMessage().contains("expired")
                ? "register.error.expired" : "register.error.alreadyUsed");
            return "register/error";
        } catch (final IllegalArgumentException e) {
            model.addAttribute("error", "register.error.invalidPassword");
            model.addAttribute("token", token);
            return "register/form";
        }
    }
}
