package de.evia.travelmate.iam.adapters.web;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.evia.travelmate.iam.application.AccountService;
import de.evia.travelmate.iam.application.command.AddDependentCommand;
import de.evia.travelmate.iam.domain.account.AccountId;

@Controller
@RequestMapping("/tenants/{tenantId}/accounts/{accountId}/dependents")
public class DependentController {

    private final AccountService accountService;

    public DependentController(final AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public String list(@PathVariable final UUID tenantId,
                       @PathVariable final UUID accountId,
                       final Model model) {
        model.addAttribute("tenantId", tenantId);
        model.addAttribute("accountId", accountId);
        model.addAttribute("dependents", accountService.findDependentsByGuardian(new AccountId(accountId)));
        return "dependent/list";
    }

    @PostMapping
    public String add(@PathVariable final UUID tenantId,
                      @PathVariable final UUID accountId,
                      @RequestParam final String firstName,
                      @RequestParam final String lastName,
                      final Model model) {
        accountService.addDependent(new AddDependentCommand(tenantId, accountId, firstName, lastName, null));
        model.addAttribute("tenantId", tenantId);
        model.addAttribute("accountId", accountId);
        model.addAttribute("dependents", accountService.findDependentsByGuardian(new AccountId(accountId)));
        return "dependent/list";
    }
}
