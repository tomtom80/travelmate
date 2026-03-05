package de.evia.travelmate.iam.adapters.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal final Jwt jwt) {
        if (jwt != null) {
            return "redirect:/dashboard";
        }
        return "landing";
    }
}
