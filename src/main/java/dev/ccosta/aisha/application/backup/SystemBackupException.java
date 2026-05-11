package dev.ccosta.aisha.application.backup;

/**
 * Signals a technical failure while preparing the system backup archive.
 */
public class SystemBackupException extends RuntimeException {

    public SystemBackupException(String message) {
        super(message);
    }

    public SystemBackupException(String message, Throwable cause) {
        super(message, cause);
    }
}
