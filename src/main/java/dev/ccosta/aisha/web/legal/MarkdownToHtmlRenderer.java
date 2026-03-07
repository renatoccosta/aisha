package dev.ccosta.aisha.web.legal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Renders a constrained Markdown subset into safe HTML for server-side pages.
 */
@Component
public class MarkdownToHtmlRenderer {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^\\d+\\.\\s+(.*)$");

    /**
     * Converts Markdown text to HTML.
     *
     * @param markdown markdown source text
     * @return rendered HTML fragment
     */
    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        String normalized = markdown.replace("\r\n", "\n");

        boolean inParagraph = false;
        boolean inUnorderedList = false;
        boolean inOrderedList = false;

        for (String line : normalized.split("\n")) {
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                inParagraph = closeParagraphIfNeeded(html, inParagraph);
                inUnorderedList = closeUnorderedListIfNeeded(html, inUnorderedList);
                inOrderedList = closeOrderedListIfNeeded(html, inOrderedList);
                continue;
            }

            Matcher headingMatcher = HEADING_PATTERN.matcher(trimmed);
            if (headingMatcher.matches()) {
                inParagraph = closeParagraphIfNeeded(html, inParagraph);
                inUnorderedList = closeUnorderedListIfNeeded(html, inUnorderedList);
                inOrderedList = closeOrderedListIfNeeded(html, inOrderedList);

                int level = headingMatcher.group(1).length();
                html.append("<h").append(level).append(">")
                    .append(escape(headingMatcher.group(2)))
                    .append("</h").append(level).append(">\n");
                continue;
            }

            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                inParagraph = closeParagraphIfNeeded(html, inParagraph);
                inOrderedList = closeOrderedListIfNeeded(html, inOrderedList);
                if (!inUnorderedList) {
                    html.append("<ul>\n");
                    inUnorderedList = true;
                }
                html.append("<li>").append(escape(trimmed.substring(2).trim())).append("</li>\n");
                continue;
            }

            Matcher orderedListMatcher = ORDERED_LIST_PATTERN.matcher(trimmed);
            if (orderedListMatcher.matches()) {
                inParagraph = closeParagraphIfNeeded(html, inParagraph);
                inUnorderedList = closeUnorderedListIfNeeded(html, inUnorderedList);
                if (!inOrderedList) {
                    html.append("<ol>\n");
                    inOrderedList = true;
                }
                html.append("<li>").append(escape(orderedListMatcher.group(1).trim())).append("</li>\n");
                continue;
            }

            inUnorderedList = closeUnorderedListIfNeeded(html, inUnorderedList);
            inOrderedList = closeOrderedListIfNeeded(html, inOrderedList);

            if (!inParagraph) {
                html.append("<p>");
                inParagraph = true;
            } else {
                html.append(' ');
            }
            html.append(escape(trimmed));
        }

        inParagraph = closeParagraphIfNeeded(html, inParagraph);
        inUnorderedList = closeUnorderedListIfNeeded(html, inUnorderedList);
        closeOrderedListIfNeeded(html, inOrderedList);

        return html.toString();
    }

    private boolean closeParagraphIfNeeded(StringBuilder html, boolean inParagraph) {
        if (inParagraph) {
            html.append("</p>\n");
        }
        return false;
    }

    private boolean closeUnorderedListIfNeeded(StringBuilder html, boolean inUnorderedList) {
        if (inUnorderedList) {
            html.append("</ul>\n");
        }
        return false;
    }

    private boolean closeOrderedListIfNeeded(StringBuilder html, boolean inOrderedList) {
        if (inOrderedList) {
            html.append("</ol>\n");
        }
        return false;
    }

    private String escape(String text) {
        return HtmlUtils.htmlEscape(text == null ? "" : text);
    }
}
