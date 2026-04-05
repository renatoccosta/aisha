package dev.ccosta.aisha.web.navigation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReturnPathSupportTest {

    @Test
    void shouldBuildReturnPathWithQueryParameters() {
        String returnPath = ReturnPathSupport.buildReturnPath(
            "/entries",
            "accountId",
            4L,
            "description",
            "cartao visa",
            "pendingSuggestions",
            true,
            "page",
            2,
            "size",
            50
        );

        assertThat(returnPath)
            .isEqualTo("/entries?accountId=4&description=cartao%20visa&pendingSuggestions=true&page=2&size=50");
    }

    @Test
    void shouldIgnoreBlankParametersWhenBuildingReturnPath() {
        String returnPath = ReturnPathSupport.buildReturnPath("/categories", "page", 1, "filter", " ", "size", 25);

        assertThat(returnPath).isEqualTo("/categories?page=1&size=25");
    }

    @Test
    void shouldRejectUnsafeExternalReturnPath() {
        String returnPath = ReturnPathSupport.resolveReturnPath("https://evil.example/test", "/accounts");

        assertThat(returnPath).isEqualTo("/accounts");
    }
}
