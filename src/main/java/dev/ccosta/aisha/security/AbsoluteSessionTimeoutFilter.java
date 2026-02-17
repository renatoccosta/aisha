package dev.ccosta.aisha.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AbsoluteSessionTimeoutFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AbsoluteSessionTimeoutFilter.class);
    private static final Duration ABSOLUTE_TIMEOUT = Duration.ofHours(12);

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isAuthenticated(authentication)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Long authenticatedAt = (Long) session.getAttribute(AuditAuthenticationHandlers.SESSION_AUTHENTICATED_AT);
                if (authenticatedAt == null) {
                    authenticatedAt = session.getCreationTime();
                    session.setAttribute(AuditAuthenticationHandlers.SESSION_AUTHENTICATED_AT, authenticatedAt);
                }

                long ageMillis = System.currentTimeMillis() - authenticatedAt;
                if (ageMillis > ABSOLUTE_TIMEOUT.toMillis()) {
                    String username = authentication.getName();
                    SecurityContextHolder.clearContext();
                    session.invalidate();

                    log.info("Absolute session timeout reached: username={}, remoteAddress={}", username, request.getRemoteAddr());

                    if ("true".equalsIgnoreCase(request.getHeader("HX-Request"))) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setHeader("HX-Redirect", request.getContextPath() + "/login?expired");
                        return;
                    }

                    response.sendRedirect(request.getContextPath() + "/login?expired");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
