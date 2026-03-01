package dev.ccosta.aisha.domain.entry;

public enum EntryCategorySuggestionStatus {
    NONE,
    PENDING,
    ACCEPTED,
    REJECTED;

    public boolean isUserValidated() {
        return this == NONE || this == ACCEPTED || this == REJECTED;
    }
}
