package dev.ccosta.aisha.application.search;

/**
 * Represents one parsed text search term and its normalized SQL LIKE pattern.
 */
public record TextSearchTerm(String likePattern) {

    public TextSearchTerm {
        if (likePattern == null || likePattern.isBlank()) {
            throw new IllegalArgumentException("Search term pattern is required");
        }
    }
}
