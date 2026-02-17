package dev.ccosta.aisha.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class AuditAuthenticationHandlers
    implements AuthenticationSuccessHandler, AuthenticationFailureHandler, LogoutSuccessHandler {

    public static final String SESSION_AUTHENTICATED_AT = "SECURITY_AUTHENTICATED_AT";
    private static final Logger log = LoggerFactory.getLogger(AuditAuthenticationHandlers.class);

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_AUTHENTICATED_AT, System.currentTimeMillis());

        log.info(
            "Authentication success: username={}, remoteAddress={}",
            authentication.getName(),
            request.getRemoteAddr()
        );

        response.sendRedirect(request.getContextPath() + "/dashboard");
    }

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        String attemptedUsername = request.getParameter("username");
        log.warn(
            "Authentication failure: username={}, remoteAddress={}, reason={}",
            attemptedUsername,
            request.getRemoteAddr(),
            exception.getClass().getSimpleName()
        );

        response.sendRedirect(request.getContextPath() + "/login?error");
    }

    @Override
    public void onLogoutSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        String username = authentication != null ? authentication.getName() : "anonymous";
        log.info("Logout success: username={}, remoteAddress={}", username, request.getRemoteAddr());

        response.sendRedirect(request.getContextPath() + "/login?logout");
    }
}
