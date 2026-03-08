package dev.ccosta.aisha.web.legal;

import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the terms of use page rendered from the markdown source.
 */
@Controller
public class TermsOfUseController {

    private final TermsOfUseContentService termsOfUseContentService;

    public TermsOfUseController(TermsOfUseContentService termsOfUseContentService) {
        this.termsOfUseContentService = termsOfUseContentService;
    }

    /**
     * Renders the terms of use page.
     *
     * @param locale active request locale used for terms language selection
     * @param model MVC model used by Thymeleaf
     * @return template for the terms of use page
     */
    @GetMapping("/terms-of-use")
    public String termsOfUse(Locale locale, Model model) {
        model.addAttribute("termsOfUseHtml", termsOfUseContentService.renderedHtml(locale));
        return "legal/terms-of-use";
    }
}
