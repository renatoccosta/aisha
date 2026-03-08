package dev.ccosta.aisha.web.legal;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Loads and renders localized terms of use markdown files from the classpath.
 */
@Service
public class TermsOfUseContentService {

    private static final String TERMS_RESOURCE_PATH_EN = "legal/terms-of-use.md";
    private static final String TERMS_RESOURCE_PATH_PT_BR = "legal/terms-of-use_pt_BR.md";
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final MarkdownToHtmlRenderer markdownToHtmlRenderer;

    private Map<Locale, String> markdownByLocale;
    private Map<Locale, String> renderedHtmlByLocale;

    public TermsOfUseContentService(MarkdownToHtmlRenderer markdownToHtmlRenderer) {
        this.markdownToHtmlRenderer = markdownToHtmlRenderer;
    }

    /**
     * Initializes cached markdown and rendered HTML once at startup.
     */
    @PostConstruct
    public void initialize() {
        try {
            String markdownEn = readResource(TERMS_RESOURCE_PATH_EN);
            String markdownPtBr = readResource(TERMS_RESOURCE_PATH_PT_BR);

            this.markdownByLocale = Map.of(
                Locale.ENGLISH, markdownEn,
                PT_BR, markdownPtBr
            );
            this.renderedHtmlByLocale = Map.of(
                Locale.ENGLISH, markdownToHtmlRenderer.render(markdownEn),
                PT_BR, markdownToHtmlRenderer.render(markdownPtBr)
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load localized terms of use from classpath", ex);
        }
    }

    /**
     * Returns localized rendered HTML terms for the web page.
     *
     * @param locale active locale for terms language selection
     * @return rendered HTML
     */
    public String renderedHtml(Locale locale) {
        return renderedHtmlByLocale.get(resolveTermsLocale(locale));
    }

    /**
     * Returns localized markdown source terms.
     *
     * @param locale active locale for terms language selection
     * @return markdown source text
     */
    public String markdownSource(Locale locale) {
        return markdownByLocale.get(resolveTermsLocale(locale));
    }

    private String readResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private Locale resolveTermsLocale(Locale locale) {
        if (locale != null && "pt".equalsIgnoreCase(locale.getLanguage()) && "BR".equalsIgnoreCase(locale.getCountry())) {
            return PT_BR;
        }
        return Locale.ENGLISH;
    }
}
