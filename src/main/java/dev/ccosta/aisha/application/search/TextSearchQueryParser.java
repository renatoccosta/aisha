package dev.ccosta.aisha.application.search;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * Parses listing text filters into a small database-safe search language.
 */
public class TextSearchQueryParser {

    private static final String DIACRITICS_PATTERN = "\\p{M}+";

    /**
     * Parses a user-provided search expression.
     *
     * @param value raw filter text typed by the user
     * @return parsed positive and negative terms, or an empty query for blank input
     */
    public TextSearchQuery parse(String value) {
        if (!StringUtils.hasText(value)) {
            return new TextSearchQuery(List.of(), List.of());
        }

        SearchScanner scanner = new SearchScanner(value.trim());
        List<TextSearchTerm> positiveTerms = new ArrayList<>();
        List<TextSearchTerm> negativeTerms = new ArrayList<>();
        ParsedToken token;
        while ((token = scanner.nextToken()) != null) {
            TextSearchTerm term = toSearchTerm(token);
            if (term == null) {
                continue;
            }
            if (token.negative()) {
                negativeTerms.add(term);
            } else {
                positiveTerms.add(term);
            }
        }
        return new TextSearchQuery(positiveTerms, negativeTerms);
    }

    /**
     * Normalizes searchable text using the same accent and case rules as parsed terms.
     *
     * @param value text to normalize
     * @return uppercase text without diacritics, or an empty string for null input
     */
    public String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll(DIACRITICS_PATTERN, "")
            .toUpperCase(Locale.ROOT);
    }

    private TextSearchTerm toSearchTerm(ParsedToken token) {
        StringBuilder pattern = new StringBuilder("%");
        StringBuilder literal = new StringBuilder();
        boolean hasContent = false;

        for (TokenCharacter character : token.characters()) {
            if (character.wildcard() == Wildcard.ANY_SEQUENCE) {
                flushLiteral(pattern, literal);
                pattern.append('%');
                hasContent = true;
            } else if (character.wildcard() == Wildcard.SINGLE_CHARACTER) {
                flushLiteral(pattern, literal);
                pattern.append('_');
                hasContent = true;
            } else {
                literal.append(character.value());
                if (!Character.isWhitespace(character.value())) {
                    hasContent = true;
                }
            }
        }

        flushLiteral(pattern, literal);
        pattern.append('%');
        if (!hasContent) {
            return null;
        }
        return new TextSearchTerm(pattern.toString());
    }

    private void flushLiteral(StringBuilder pattern, StringBuilder literal) {
        if (literal.isEmpty()) {
            return;
        }
        pattern.append(escapeLikePattern(normalizeText(literal.toString())));
        literal.setLength(0);
    }

    private String escapeLikePattern(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }

    private enum Wildcard {
        NONE,
        ANY_SEQUENCE,
        SINGLE_CHARACTER
    }

    private record TokenCharacter(char value, Wildcard wildcard) {
        private static TokenCharacter literal(char value) {
            return new TokenCharacter(value, Wildcard.NONE);
        }

        private static TokenCharacter wildcard(Wildcard wildcard) {
            return new TokenCharacter('\0', wildcard);
        }
    }

    private record ParsedToken(boolean negative, List<TokenCharacter> characters) {
    }

    private static final class SearchScanner {

        private final String value;
        private int index;

        private SearchScanner(String value) {
            this.value = value;
        }

        private ParsedToken nextToken() {
            skipWhitespace();
            if (index >= value.length()) {
                return null;
            }

            boolean negative = consumeNegativePrefix();
            List<TokenCharacter> characters = index < value.length() && value.charAt(index) == '"'
                ? readQuotedToken()
                : readPlainToken();
            return new ParsedToken(negative, characters);
        }

        private boolean consumeNegativePrefix() {
            if (value.charAt(index) != '-' || index + 1 >= value.length() || Character.isWhitespace(value.charAt(index + 1))) {
                return false;
            }
            index++;
            return true;
        }

        private List<TokenCharacter> readQuotedToken() {
            index++;
            List<TokenCharacter> characters = new ArrayList<>();
            while (index < value.length()) {
                char current = value.charAt(index++);
                if (current == '\\') {
                    if (index < value.length()) {
                        characters.add(TokenCharacter.literal(value.charAt(index++)));
                    } else {
                        characters.add(TokenCharacter.literal(current));
                    }
                } else if (current == '"') {
                    break;
                } else {
                    addCharacter(characters, current);
                }
            }
            return characters;
        }

        private List<TokenCharacter> readPlainToken() {
            List<TokenCharacter> characters = new ArrayList<>();
            while (index < value.length() && !Character.isWhitespace(value.charAt(index))) {
                char current = value.charAt(index++);
                if (current == '\\') {
                    if (index < value.length()) {
                        characters.add(TokenCharacter.literal(value.charAt(index++)));
                    } else {
                        characters.add(TokenCharacter.literal(current));
                    }
                } else {
                    addCharacter(characters, current);
                }
            }
            return characters;
        }

        private void addCharacter(List<TokenCharacter> characters, char current) {
            if (current == '*') {
                characters.add(TokenCharacter.wildcard(Wildcard.ANY_SEQUENCE));
            } else if (current == '?') {
                characters.add(TokenCharacter.wildcard(Wildcard.SINGLE_CHARACTER));
            } else {
                characters.add(TokenCharacter.literal(current));
            }
        }

        private void skipWhitespace() {
            while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
                index++;
            }
        }
    }
}
