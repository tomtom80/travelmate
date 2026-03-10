package de.evia.travelmate.iam.adapters.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.evia.travelmate.iam.application.SignUpService;
import de.evia.travelmate.iam.application.command.SignUpCommand;

@Controller
@RequestMapping("/signup")
public class SignUpController {

    private final SignUpService signUpService;

    public SignUpController(final SignUpService signUpService) {
        this.signUpService = signUpService;
    }

    @GetMapping
    public String showSignUpForm(final Model model) {
        model.addAttribute("view", "signup/form");
        return "layout/public";
    }

    @PostMapping
    public String signUp(@RequestParam final String tenantName,
                         @RequestParam final String firstName,
                         @RequestParam final String lastName,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate dateOfBirth,
                         @RequestParam final String email,
                         @RequestParam final String password,
                         @RequestParam final String passwordConfirm,
                         final Model model) {
        if (!password.equals(passwordConfirm)) {
            model.addAttribute("view", "signup/form");
            model.addAttribute("error", "signup.error.passwordMismatch");
            model.addAttribute("tenantName", tenantName);
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("dateOfBirth", dateOfBirth);
            model.addAttribute("email", email);
            return "layout/public";
        }

        try {
            signUpService.signUp(new SignUpCommand(tenantName, firstName, lastName, email, password, dateOfBirth));
            model.addAttribute("view", "signup/success");
            return "layout/public";
        } catch (final IllegalArgumentException e) {
            model.addAttribute("view", "signup/form");
            model.addAttribute("error", "signup.error.tenantExists");
            model.addAttribute("tenantName", tenantName);
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("dateOfBirth", dateOfBirth);
            model.addAttribute("email", email);
            return "layout/public";
        }
    }
}
