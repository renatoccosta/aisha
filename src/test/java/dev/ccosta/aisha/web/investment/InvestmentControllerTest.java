package dev.ccosta.aisha.web.investment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InvestmentControllerTest {

    private final InvestmentController investmentController = new InvestmentController();

    @Test
    void shouldRenderInvestmentLandingPage() {
        String view = investmentController.index();

        assertThat(view).isEqualTo("investments/index");
    }
}
