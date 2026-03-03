package de.evia.travelmate.iam.adapters.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(final Model model) {
        model.addAttribute("title", "IAM");
        model.addAttribute("view", "index");
        return "layout/default";
    }
}
