package dev.ccosta.aisha.web.legal;

import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the privacy policy page rendered from the markdown source.
 */
@Controller
public class PrivacyPolicyController {

    private final PrivacyPolicyContentService privacyPolicyContentService;

    public PrivacyPolicyController(PrivacyPolicyContentService privacyPolicyContentService) {
        this.privacyPolicyContentService = privacyPolicyContentService;
    }

    /**
     * Renders the privacy policy page.
     *
     * @param locale active request locale used for policy language selection
     * @param model MVC model used by Thymeleaf
     * @return template for the privacy policy page
     */
    @GetMapping("/privacy-policy")
    public String privacyPolicy(Locale locale, Model model) {
        model.addAttribute("privacyPolicyHtml", privacyPolicyContentService.renderedHtml(locale));
        return "legal/privacy-policy";
    }
}
