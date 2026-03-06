package dev.ccosta.aisha.web.auth;

import dev.ccosta.aisha.security.FederatedAuthPendingLink;
import dev.ccosta.aisha.security.FederatedAuthenticationFailureHandler;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class LoginController {

    @ModelAttribute("federatedLinkForm")
    public FederatedLinkConfirmationForm federatedLinkConfirmationForm() {
        return new FederatedLinkConfirmationForm();
    }

    @GetMapping("/login")
    public String login(Authentication authentication, HttpSession session, Model model) {
        if (authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }

        if (session != null) {
            FederatedAuthPendingLink pendingLink =
                (FederatedAuthPendingLink) session.getAttribute(FederatedAuthenticationFailureHandler.SESSION_PENDING_FEDERATED_LINK);
            if (pendingLink != null) {
                model.addAttribute("pendingFederatedLink", pendingLink);
            }
        }

        return "auth/login";
    }
}
