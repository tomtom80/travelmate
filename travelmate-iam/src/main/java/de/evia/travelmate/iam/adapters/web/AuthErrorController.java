package de.evia.travelmate.iam.adapters.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class AuthErrorController {

    @GetMapping("/auth-error")
    public String authError(final Model model) {
        model.addAttribute("title", "Anmeldung fehlgeschlagen");
        model.addAttribute("view", "auth-error/index");
        return "layout/public";
    }
}
