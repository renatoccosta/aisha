package dev.ccosta.aisha.application.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextSearchQueryParserTest {

    private final TextSearchQueryParser parser = new TextSearchQueryParser();

    @Test
    void shouldParsePositiveTermsAsOrCandidates() {
        TextSearchQuery query = parser.parse("mercado padaria");

        assertThat(query.positiveTerms())
            .extracting(TextSearchTerm::likePattern)
            .containsExactly("%MERCADO%", "%PADARIA%");
        assertThat(query.negativeTerms()).isEmpty();
    }

    @Test
    void shouldParseQuotedPhrasesAndNegativeTerms() {
        TextSearchQuery query = parser.parse("\"café da manhã\" -ifood");

        assertThat(query.positiveTerms())
            .extracting(TextSearchTerm::likePattern)
            .containsExactly("%CAFE DA MANHA%");
        assertThat(query.negativeTerms())
            .extracting(TextSearchTerm::likePattern)
            .containsExactly("%IFOOD%");
    }

    @Test
    void shouldConvertUnescapedWildcardsAndEscapeLiteralLikeCharacters() {
        TextSearchQuery query = parser.parse("caf* cart?o taxa\\* valor\\? a\\_b 100\\%");

        assertThat(query.positiveTerms())
            .extracting(TextSearchTerm::likePattern)
            .containsExactly("%CAF%%", "%CART_O%", "%TAXA*%", "%VALOR?%", "%A\\_B%", "%100\\%%");
    }

    @Test
    void shouldReturnEmptyQueryForBlankInput() {
        assertThat(parser.parse("   ").hasTerms()).isFalse();
    }
}
