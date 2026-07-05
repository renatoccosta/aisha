package dev.ccosta.aisha.application.search;

import java.util.List;

/**
 * Represents a parsed text search expression with inclusive and exclusive terms.
 */
public record TextSearchQuery(List<TextSearchTerm> positiveTerms, List<TextSearchTerm> negativeTerms) {

    public TextSearchQuery {
        positiveTerms = List.copyOf(positiveTerms == null ? List.of() : positiveTerms);
        negativeTerms = List.copyOf(negativeTerms == null ? List.of() : negativeTerms);
    }

    /**
     * Indicates whether the query has at least one parsed term.
     *
     * @return true when positive or negative terms are present
     */
    public boolean hasTerms() {
        return !positiveTerms.isEmpty() || !negativeTerms.isEmpty();
    }
}
