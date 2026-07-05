package dev.ccosta.aisha.infrastructure.persistence.search;

import dev.ccosta.aisha.application.search.TextSearchQuery;
import dev.ccosta.aisha.application.search.TextSearchTerm;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds JPA predicates for the reusable listing text search language.
 */
public final class TextSearchPredicateBuilder {

    private static final char LIKE_ESCAPE = '\\';
    private static final String ACCENTED_CHARACTERS = "ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑáàâãäéèêëíìîïóòôõöúùûüçñ";
    private static final String ASCII_CHARACTERS = "AAAAAEEEEIIIIOOOOOUUUUCNaaaaaeeeeiiiiooooouuuucn";

    private TextSearchPredicateBuilder() {
    }

    /**
     * Creates a predicate from parsed search terms against a normalized text expression.
     *
     * @param cb criteria builder
     * @param textExpression expression containing the text fields to search
     * @param query parsed search query
     * @return a predicate, or null when the query has no terms
     */
    public static Predicate build(CriteriaBuilder cb, Expression<String> textExpression, TextSearchQuery query) {
        if (query == null || !query.hasTerms()) {
            return null;
        }

        Expression<String> normalizedText = cb.upper(
            cb.function(
                "translate",
                String.class,
                cb.coalesce(textExpression, ""),
                cb.literal(ACCENTED_CHARACTERS),
                cb.literal(ASCII_CHARACTERS)
            )
        );
        List<Predicate> predicates = new ArrayList<>();
        if (!query.positiveTerms().isEmpty()) {
            predicates.add(cb.or(query.positiveTerms().stream()
                .map(term -> like(cb, normalizedText, term))
                .toArray(Predicate[]::new)));
        }
        query.negativeTerms().stream()
            .map(term -> cb.not(like(cb, normalizedText, term)))
            .forEach(predicates::add);

        return cb.and(predicates.toArray(Predicate[]::new));
    }

    private static Predicate like(CriteriaBuilder cb, Expression<String> text, TextSearchTerm term) {
        return cb.like(text, term.likePattern(), LIKE_ESCAPE);
    }
}
