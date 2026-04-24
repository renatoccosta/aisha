package dev.ccosta.aisha.application.investment;

/**
 * Signals that an investment operation references an unknown imported brokerage note.
 */
public class BrokerageNoteNotFoundException extends RuntimeException {

    public BrokerageNoteNotFoundException(Long id) {
        super("Brokerage note not found: " + id);
    }
}
