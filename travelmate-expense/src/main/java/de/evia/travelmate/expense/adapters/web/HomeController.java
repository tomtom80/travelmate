package de.evia.travelmate.expense.adapters.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(final Model model) {
        model.addAttribute("title", "Abrechnung");
        model.addAttribute("view", "index");
        return "layout/default";
    }
}
