package dev.ccosta.aisha.web;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the application version to all MVC views.
 * <p>
 * The version is read from Spring Boot {@link BuildProperties}, generated during the Maven build.
 * When build metadata is unavailable (common during local development runs), a fallback label is used.
 */
@ControllerAdvice
public class ApplicationVersionModelAdvice {

    private final Optional<BuildProperties> buildProperties;
    private final String fallbackVersionLabel;

    public ApplicationVersionModelAdvice(
        Optional<BuildProperties> buildProperties,
        @Value("${aisha.app.version-fallback:desenvolvimento}") String fallbackVersionLabel
    ) {
        this.buildProperties = buildProperties;
        this.fallbackVersionLabel = fallbackVersionLabel;
    }

    /**
     * Returns the application version to be displayed in shared templates.
     *
     * @return build version from generated build metadata, or a development fallback label when metadata is absent.
     */
    @ModelAttribute("applicationVersion")
    public String applicationVersion() {
        return buildProperties
            .map(BuildProperties::getVersion)
            .filter(StringUtils::hasText)
            .orElse(fallbackVersionLabel);
    }
}
