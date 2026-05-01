package dev.ccosta.aisha.web.finance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FinanceControllerTest {

    private final FinanceController financeController = new FinanceController();

    @Test
    void shouldRenderFinanceLandingPage() {
        String view = financeController.index();

        assertThat(view).isEqualTo("finances/index");
    }
}
