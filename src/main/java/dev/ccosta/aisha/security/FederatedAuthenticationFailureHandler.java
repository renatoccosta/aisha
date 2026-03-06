package dev.ccosta.aisha.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Handles OAuth2 failures and stores pending account-link state when password confirmation is required.
 */
@Component
public class FederatedAuthenticationFailureHandler extends AuditAuthenticationHandlers {

    private static final String ACCOUNT_LINK_REQUIRED = "account_link_required";
    public static final String SESSION_PENDING_FEDERATED_LINK = "SECURITY_PENDING_FEDERATED_LINK";

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception
            && ACCOUNT_LINK_REQUIRED.equals(oauth2Exception.getError().getErrorCode())) {
            String[] pieces = oauth2Exception.getError().getDescription().split("\\|", 3);
            if (pieces.length == 3) {
                HttpSession session = request.getSession(true);
                session.setAttribute(
                    SESSION_PENDING_FEDERATED_LINK,
                    new FederatedAuthPendingLink(pieces[0], pieces[1], pieces[2])
                );
                response.sendRedirect(request.getContextPath() + "/login?linkRequired");
                return;
            }
        }

        super.onAuthenticationFailure(request, response, exception);
    }
}
