package dev.ccosta.aisha.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

class ApplicationVersionModelAdviceTest {

    @Test
    void shouldReturnBuildVersionWhenAvailable() {
        ApplicationVersionModelAdvice advice = new ApplicationVersionModelAdvice(
            Optional.of(buildPropertiesWithVersion("1.4.2")),
            "desenvolvimento"
        );

        assertThat(advice.applicationVersion()).isEqualTo("1.4.2");
    }

    @Test
    void shouldReturnFallbackWhenBuildVersionIsBlank() {
        ApplicationVersionModelAdvice advice = new ApplicationVersionModelAdvice(
            Optional.of(buildPropertiesWithVersion("")),
            "desenvolvimento"
        );

        assertThat(advice.applicationVersion()).isEqualTo("desenvolvimento");
    }

    @Test
    void shouldReturnFallbackWhenBuildPropertiesAreMissing() {
        ApplicationVersionModelAdvice advice = new ApplicationVersionModelAdvice(
            Optional.empty(),
            "desenvolvimento"
        );

        assertThat(advice.applicationVersion()).isEqualTo("desenvolvimento");
    }

    private static BuildProperties buildPropertiesWithVersion(String version) {
        Properties properties = new Properties();
        properties.setProperty("version", version);
        return new BuildProperties(properties);
    }
}
