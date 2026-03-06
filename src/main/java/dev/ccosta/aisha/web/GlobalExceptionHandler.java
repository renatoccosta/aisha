package dev.ccosta.aisha.web;

import dev.ccosta.aisha.infrastructure.logging.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public String handleUnexpectedError(Exception ex, HttpServletRequest request, Model model) {
        String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY));
        log.error(
            "Unhandled error returned to user. correlationId={}, method={}, path={}",
            correlationId,
            request.getMethod(),
            request.getRequestURI(),
            ex
        );
        model.addAttribute("errorCorrelationId", correlationId);
        model.addAttribute("errorBackUrl", resolveSafeBackUrl(request));
        return "errors/500";
    }

    private String resolveSafeBackUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/dashboard";
        }

        try {
            URI refererUri = new URI(referer);
            String candidate = extractPathAndQuery(refererUri);
            if (candidate == null || candidate.equals(request.getRequestURI()) || candidate.startsWith("/error")) {
                return "/dashboard";
            }

            if (!refererUri.isAbsolute()) {
                return candidate;
            }

            String refererHost = refererUri.getHost();
            if (refererHost != null && refererHost.equalsIgnoreCase(request.getServerName())) {
                return candidate;
            }
        } catch (URISyntaxException ex) {
            log.debug("Ignoring invalid Referer header while resolving error back URL. referer={}", referer);
        }
        return "/dashboard";
    }

    private String extractPathAndQuery(URI uri) {
        if (uri.getPath() == null || uri.getPath().isBlank() || !uri.getPath().startsWith("/")) {
            return null;
        }
        if (uri.getRawQuery() == null || uri.getRawQuery().isBlank()) {
            return uri.getPath();
        }
        return uri.getPath() + "?" + uri.getRawQuery();
    }
}
