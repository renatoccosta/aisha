package dev.ccosta.aisha.web.finance;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Provides the finance module landing route.
 */
@Controller
public class FinanceController {

    /**
     * Displays the finance landing page with the available operational sections.
     *
     * @return the finance landing page template
     */
    @GetMapping("/finances")
    public String index() {
        return "finances/index";
    }
}
