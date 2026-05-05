package de.evia.travelmate.expense.adapters.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
class NavbarModelAdvice {

    @ModelAttribute("navUserName")
    String navUserName(@AuthenticationPrincipal final Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        final String given = jwt.getClaimAsString("given_name");
        final String family = jwt.getClaimAsString("family_name");
        if (given != null && family != null) {
            return given + " " + family;
        }
        return given != null ? given : jwt.getClaimAsString("preferred_username");
    }
}
