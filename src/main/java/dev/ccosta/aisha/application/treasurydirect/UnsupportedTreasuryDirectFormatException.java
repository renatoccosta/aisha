package dev.ccosta.aisha.application.treasurydirect;

/**
 * Indicates that an uploaded file does not match the supported Treasury Direct operations format.
 */
public class UnsupportedTreasuryDirectFormatException extends RuntimeException {

    public UnsupportedTreasuryDirectFormatException(String message) {
        super(message);
    }

    public UnsupportedTreasuryDirectFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
