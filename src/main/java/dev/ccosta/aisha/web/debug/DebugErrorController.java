package dev.ccosta.aisha.web.debug;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exposes development-only endpoints used to manually validate global error handling pages.
 */
@Controller
@RequestMapping("/debug")
@Profile("!prod")
@ConditionalOnProperty(prefix = "aisha.debug", name = "force-error-endpoint-enabled", havingValue = "true")
public class DebugErrorController {

    /**
     * Forces an unexpected server error so the 500 error page can be validated in a browser.
     *
     * @return never returns normally because it always throws an exception
     */
    @GetMapping("/force-error")
    public String forceInternalServerError() {
        throw new IllegalStateException("Forced error for manual 500 page testing");
    }

    /**
     * Forces a not found error so the 404 page can be validated in a browser.
     *
     * @return the not found page template
     */
    @GetMapping("/force-not-found")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String forceNotFound() {
        return "errors/404";
    }
}
