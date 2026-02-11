package dev.ccosta.aisha.application.entry;

public class EntryNotFoundException extends RuntimeException {

    public EntryNotFoundException(Long id) {
        super("Entry not found: " + id);
    }
}
