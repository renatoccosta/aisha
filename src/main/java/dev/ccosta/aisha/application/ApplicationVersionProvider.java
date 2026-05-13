package dev.ccosta.aisha.application;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves the application version used in UI displays and operational metadata.
 */
@Component
public class ApplicationVersionProvider {

    private final Optional<BuildProperties> buildProperties;
    private final String fallbackVersionLabel;

    public ApplicationVersionProvider(
        Optional<BuildProperties> buildProperties,
        @Value("${aisha.app.version-fallback:desenvolvimento}") String fallbackVersionLabel
    ) {
        this.buildProperties = buildProperties;
        this.fallbackVersionLabel = fallbackVersionLabel;
    }

    /**
     * Returns the current application version from build metadata or a configured fallback label.
     *
     * @return version string suitable for UI and backup manifests
     */
    public String currentVersion() {
        return buildProperties
            .map(BuildProperties::getVersion)
            .filter(StringUtils::hasText)
            .orElse(fallbackVersionLabel);
    }
}
