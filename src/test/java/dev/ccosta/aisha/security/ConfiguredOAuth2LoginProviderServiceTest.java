package dev.ccosta.aisha.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;

class ConfiguredOAuth2LoginProviderServiceTest {

    @Test
    void shouldHideProviderWhenCredentialsArePlaceholders() {
        OAuth2ClientProperties properties = new OAuth2ClientProperties();
        OAuth2ClientProperties.Registration google = new OAuth2ClientProperties.Registration();
        google.setClientId("change-me-google-client-id");
        google.setClientSecret("change-me-google-client-secret");
        properties.getRegistration().put("google", google);

        ConfiguredOAuth2LoginProviderService service = new ConfiguredOAuth2LoginProviderService(properties);

        assertThat(service.enabledProviders()).isEmpty();
    }

    @Test
    void shouldExposeProviderWhenCredentialsAreConfigured() {
        OAuth2ClientProperties properties = new OAuth2ClientProperties();
        OAuth2ClientProperties.Registration google = new OAuth2ClientProperties.Registration();
        google.setClientId("google-client-id");
        google.setClientSecret("google-client-secret");
        properties.getRegistration().put("google", google);

        ConfiguredOAuth2LoginProviderService service = new ConfiguredOAuth2LoginProviderService(properties);

        assertThat(service.enabledProviders())
            .singleElement()
            .extracting(
                ConfiguredOAuth2LoginProviderService.ExternalLoginProviderView::registrationId,
                ConfiguredOAuth2LoginProviderService.ExternalLoginProviderView::authorizationUri,
                ConfiguredOAuth2LoginProviderService.ExternalLoginProviderView::labelMessageKey,
                ConfiguredOAuth2LoginProviderService.ExternalLoginProviderView::icon
            )
            .containsExactly(
                "google",
                "/oauth2/authorization/google",
                "auth.login.action.provider.google",
                "circle-user-round"
            );
    }

    @Test
    void shouldHideProviderWhenClientSecretIsBlank() {
        OAuth2ClientProperties properties = new OAuth2ClientProperties();
        OAuth2ClientProperties.Registration google = new OAuth2ClientProperties.Registration();
        google.setClientId("google-client-id");
        google.setClientSecret("  ");
        properties.getRegistration().put("google", google);

        ConfiguredOAuth2LoginProviderService service = new ConfiguredOAuth2LoginProviderService(properties);

        assertThat(service.enabledProviders()).isEmpty();
    }
}
