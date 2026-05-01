package dev.ccosta.aisha.web.investment;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Provides the investment module landing route.
 */
@Controller
public class InvestmentController {

    /**
     * Displays the investment landing page with the available operational sections.
     *
     * @return the investment landing page template
     */
    @GetMapping("/investments")
    public String index() {
        return "investments/index";
    }
}
