package dev.ccosta.aisha.web.auth;

import dev.ccosta.aisha.infrastructure.persistence.security.LocalUserAccount;
import dev.ccosta.aisha.security.AishaPrincipal;
import dev.ccosta.aisha.security.AuditAuthenticationHandlers;
import dev.ccosta.aisha.security.FederatedAuthPendingLink;
import dev.ccosta.aisha.security.FederatedAuthenticationFailureHandler;
import dev.ccosta.aisha.security.FederatedAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles local password confirmation for linking an external identity to an existing local account.
 */
@Controller
public class FederatedAuthController {

    private final FederatedAuthenticationService federatedAuthenticationService;

    public FederatedAuthController(FederatedAuthenticationService federatedAuthenticationService) {
        this.federatedAuthenticationService = federatedAuthenticationService;
    }

    @PostMapping("/auth/federated/link")
    public String confirmLink(
        @Valid @ModelAttribute("federatedLinkForm") FederatedLinkConfirmationForm form,
        BindingResult bindingResult,
        HttpServletRequest request,
        HttpServletResponse response,
        RedirectAttributes redirectAttributes
    ) {
        HttpSession session = request.getSession(false);
        FederatedAuthPendingLink pendingLink = session == null
            ? null
            : (FederatedAuthPendingLink) session.getAttribute(FederatedAuthenticationFailureHandler.SESSION_PENDING_FEDERATED_LINK);

        if (pendingLink == null || bindingResult.hasErrors()) {
            redirectAttributes.addAttribute("linkRequired", true);
            redirectAttributes.addAttribute("linkError", true);
            return "redirect:/login";
        }

        try {
            LocalUserAccount localAccount = federatedAuthenticationService.linkWithLocalPassword(pendingLink, form.getPassword());
            session.removeAttribute(FederatedAuthenticationFailureHandler.SESSION_PENDING_FEDERATED_LINK);

            AishaPrincipal principal = new AishaPrincipal(
                localAccount.getId(),
                localAccount.getUsername(),
                localAccount.getPasswordHash(),
                localAccount.isEnabled(),
                AuthorityUtils.createAuthorityList("ROLE_USER"),
                java.util.Map.of("provider", pendingLink.provider()),
                null,
                null
            );

            UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            session.setAttribute(AuditAuthenticationHandlers.SESSION_AUTHENTICATED_AT, System.currentTimeMillis());
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);

            return "redirect:/dashboard";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addAttribute("linkRequired", true);
            redirectAttributes.addAttribute("linkError", true);
            return "redirect:/login";
        }
    }
}
