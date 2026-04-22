package dev.ccosta.aisha.domain.entry.categorization;

public enum EntryCategorySuggestionStatus {
    NONE,
    PENDING,
    ACCEPTED,
    REJECTED;

    public boolean isUserValidated() {
        return this == NONE || this == ACCEPTED || this == REJECTED;
    }
}
