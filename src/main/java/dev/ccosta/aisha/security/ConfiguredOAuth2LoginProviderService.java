package dev.ccosta.aisha.security;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Resolves which external OAuth2/OIDC login providers are effectively configured and safe to expose in the login UI.
 */
@Service
public class ConfiguredOAuth2LoginProviderService {

    private static final String PLACEHOLDER_PREFIX = "change-me";
    private static final String DEFAULT_ICON = "circle-user-round";
    private static final Map<String, String> PROVIDER_ICONS = Map.of(
        "google", "circle-user-round"
    );

    private final OAuth2ClientProperties oauth2ClientProperties;

    public ConfiguredOAuth2LoginProviderService(OAuth2ClientProperties oauth2ClientProperties) {
        this.oauth2ClientProperties = oauth2ClientProperties;
    }

    /**
     * Returns enabled OAuth2 login providers sorted by registration id.
     * A provider is enabled only when both client credentials are present and not using placeholder values.
     *
     * @return immutable list of enabled external providers for login.
     */
    public List<ExternalLoginProviderView> enabledProviders() {
        return oauth2ClientProperties.getRegistration().entrySet().stream()
            .filter(entry -> hasEffectiveClientCredentials(entry.getKey(), entry.getValue()))
            .map(entry -> toView(entry.getKey()))
            .sorted((left, right) -> left.registrationId().compareTo(right.registrationId()))
            .toList();
    }

    private ExternalLoginProviderView toView(String registrationId) {
        return new ExternalLoginProviderView(
            registrationId,
            "/oauth2/authorization/" + registrationId,
            "auth.login.action.provider." + registrationId,
            PROVIDER_ICONS.getOrDefault(registrationId, DEFAULT_ICON)
        );
    }

    private boolean hasEffectiveClientCredentials(String registrationId, OAuth2ClientProperties.Registration registration) {
        if (registration == null) {
            return false;
        }

        return isEffectiveClientValue(registrationId, registration.getClientId(), "client-id")
            && isEffectiveClientValue(registrationId, registration.getClientSecret(), "client-secret");
    }

    private boolean isEffectiveClientValue(String registrationId, String value, String suffix) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        String providerPlaceholder = (PLACEHOLDER_PREFIX + "-" + registrationId + "-" + suffix).toLowerCase(Locale.ROOT);
        return !normalizedValue.equals(PLACEHOLDER_PREFIX)
            && !normalizedValue.equals(providerPlaceholder)
            && !normalizedValue.startsWith(PLACEHOLDER_PREFIX + "-");
    }

    /**
     * View model for an external login provider rendered in the login page.
     *
     * @param registrationId OAuth2 registration identifier.
     * @param authorizationUri authorization endpoint path for Spring Security OAuth2 login.
     * @param labelMessageKey i18n message key used as button label.
     * @param icon Lucide icon name displayed alongside the provider label.
     */
    public record ExternalLoginProviderView(
        String registrationId,
        String authorizationUri,
        String labelMessageKey,
        String icon
    ) {
    }
}
