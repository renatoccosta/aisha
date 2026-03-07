package dev.ccosta.aisha.web.legal;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Loads and renders localized privacy policy markdown files from the classpath.
 */
@Service
public class PrivacyPolicyContentService {

    private static final String POLICY_RESOURCE_PATH_EN = "legal/privacy-policy.md";
    private static final String POLICY_RESOURCE_PATH_PT_BR = "legal/privacy-policy_pt_BR.md";
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final MarkdownToHtmlRenderer markdownToHtmlRenderer;

    private Map<Locale, String> markdownByLocale;
    private Map<Locale, String> renderedHtmlByLocale;

    public PrivacyPolicyContentService(MarkdownToHtmlRenderer markdownToHtmlRenderer) {
        this.markdownToHtmlRenderer = markdownToHtmlRenderer;
    }

    /**
     * Initializes cached markdown and rendered HTML once at startup.
     */
    @PostConstruct
    public void initialize() {
        try {
            String markdownEn = readResource(POLICY_RESOURCE_PATH_EN);
            String markdownPtBr = readResource(POLICY_RESOURCE_PATH_PT_BR);

            this.markdownByLocale = Map.of(
                Locale.ENGLISH, markdownEn,
                PT_BR, markdownPtBr
            );
            this.renderedHtmlByLocale = Map.of(
                Locale.ENGLISH, markdownToHtmlRenderer.render(markdownEn),
                PT_BR, markdownToHtmlRenderer.render(markdownPtBr)
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load localized privacy policies from classpath", ex);
        }
    }

    /**
     * Returns localized rendered HTML policy for the web page.
     *
     * @param locale active locale for policy selection
     * @return rendered HTML
     */
    public String renderedHtml(Locale locale) {
        return renderedHtmlByLocale.get(resolvePolicyLocale(locale));
    }

    /**
     * Returns localized markdown source policy.
     *
     * @param locale active locale for policy selection
     * @return markdown source text
     */
    public String markdownSource(Locale locale) {
        return markdownByLocale.get(resolvePolicyLocale(locale));
    }

    private String readResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private Locale resolvePolicyLocale(Locale locale) {
        if (locale != null && "pt".equalsIgnoreCase(locale.getLanguage()) && "BR".equalsIgnoreCase(locale.getCountry())) {
            return PT_BR;
        }
        return Locale.ENGLISH;
    }
}
