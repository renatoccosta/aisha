package dev.ccosta.aisha.web.investment;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Provides the investment module landing route.
 */
@Controller
public class InvestmentController {

    /**
     * Redirects the module entry point to the first implemented investment workflow.
     *
     * @return redirect to the asset listing
     */
    @GetMapping("/investments")
    public String index() {
        return "redirect:/investments/assets";
    }
}
