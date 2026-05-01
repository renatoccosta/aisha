package dev.ccosta.aisha.application.investment.importing;

/**
 * Signals that no available brokerage note processor supports the uploaded file format.
 */
public class UnsupportedBrokerageNoteFormatException extends RuntimeException {

    /**
     * Creates an exception for an unsupported brokerage note file.
     *
     * @param originalFileName uploaded file name
     */
    public UnsupportedBrokerageNoteFormatException(String originalFileName) {
        super("Unsupported brokerage note file format: " + originalFileName);
    }
}
