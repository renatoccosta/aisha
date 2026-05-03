package dev.ccosta.aisha.application.entry;

public class EntryInUseException extends RuntimeException {

    public EntryInUseException(Long id) {
        super("Entry is in use and cannot be deleted: " + id);
    }
}
