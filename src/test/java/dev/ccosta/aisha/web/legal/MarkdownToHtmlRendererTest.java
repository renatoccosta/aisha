package dev.ccosta.aisha.web.legal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownToHtmlRendererTest {

    private final MarkdownToHtmlRenderer renderer = new MarkdownToHtmlRenderer();

    @Test
    void shouldRenderHeadingsParagraphsAndLists() {
        String markdown = "# Titulo\n\nTexto inicial.\n\n- Item A\n- Item B\n\n1. Primeiro\n2. Segundo\n";

        String html = renderer.render(markdown);

        assertThat(html).contains("<h1>Titulo</h1>");
        assertThat(html).contains("<p>Texto inicial.</p>");
        assertThat(html).contains("<ul>");
        assertThat(html).contains("<li>Item A</li>");
        assertThat(html).contains("<ol>");
        assertThat(html).contains("<li>Primeiro</li>");
    }

    @Test
    void shouldEscapeHtmlInSource() {
        String markdown = "# <script>alert('x')</script>";

        String html = renderer.render(markdown);

        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;");
    }
}
