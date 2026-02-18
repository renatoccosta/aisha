package dev.ccosta.aisha.application.entry;

public class EntryImportValidationException extends RuntimeException {

    private final int rowPosition;
    private final EntryImportFailureCause causeType;

    public EntryImportValidationException(int rowPosition, EntryImportFailureCause causeType, String message) {
        super(message);
        this.rowPosition = rowPosition;
        this.causeType = causeType;
    }

    public int getRowPosition() {
        return rowPosition;
    }

    public EntryImportFailureCause getCauseType() {
        return causeType;
    }
}
