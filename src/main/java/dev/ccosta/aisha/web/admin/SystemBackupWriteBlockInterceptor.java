package dev.ccosta.aisha.web.admin;

import dev.ccosta.aisha.application.backup.SystemBackupCoordinator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Blocks data-changing HTTP requests while a system backup is running.
 */
@Component
public class SystemBackupWriteBlockInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SystemBackupWriteBlockInterceptor.class);
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final SystemBackupCoordinator backupCoordinator;

    public SystemBackupWriteBlockInterceptor(SystemBackupCoordinator backupCoordinator) {
        this.backupCoordinator = backupCoordinator;
    }

    /**
     * Rejects write requests during backup execution, except operational routes that do not mutate domain data.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param handler selected handler
     * @return true when the request may continue
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!isWriteMethod(request.getMethod()) || isAllowedOperationalPath(request.getRequestURI())) {
            return true;
        }

        if (!backupCoordinator.backupRunning()) {
            return true;
        }

        log.warn("Blocked data-changing request while system backup is running. method={}, path={}", request.getMethod(), request.getRequestURI());
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "System backup is running");
        return false;
    }

    private boolean isWriteMethod(String method) {
        return WRITE_METHODS.contains(method);
    }

    private boolean isAllowedOperationalPath(String path) {
        return path.startsWith("/admin/system-backup")
            || path.startsWith("/admin/fragments/system-backup-status")
            || path.startsWith("/date-filter")
            || path.startsWith("/logout")
            || path.startsWith("/login")
            || path.startsWith("/auth/federated");
    }
}
