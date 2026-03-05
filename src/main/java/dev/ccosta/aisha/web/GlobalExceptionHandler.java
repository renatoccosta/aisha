package dev.ccosta.aisha.web;

import dev.ccosta.aisha.infrastructure.logging.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
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
        return "errors/500";
    }
}
