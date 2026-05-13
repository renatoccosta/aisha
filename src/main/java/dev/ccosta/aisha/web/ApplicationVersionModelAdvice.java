package dev.ccosta.aisha.web;

import dev.ccosta.aisha.application.ApplicationVersionProvider;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
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

    private final ApplicationVersionProvider applicationVersionProvider;

    public ApplicationVersionModelAdvice(
        Optional<BuildProperties> buildProperties,
        @Value("${aisha.app.version-fallback:desenvolvimento}") String fallbackVersionLabel
    ) {
        this(new ApplicationVersionProvider(buildProperties, fallbackVersionLabel));
    }

    @Autowired
    public ApplicationVersionModelAdvice(ApplicationVersionProvider applicationVersionProvider) {
        this.applicationVersionProvider = applicationVersionProvider;
    }

    /**
     * Returns the application version to be displayed in shared templates.
     *
     * @return build version from generated build metadata, or a development fallback label when metadata is absent.
     */
    @ModelAttribute("applicationVersion")
    public String applicationVersion() {
        return applicationVersionProvider.currentVersion();
    }
}
