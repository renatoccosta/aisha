package dev.ccosta.aisha.application.entry.importing;

public class EntryImportValidationException extends RuntimeException {

    private final int rowPosition;
    private final EntryImportFailureCause causeType;
    private final String columnName;

    public EntryImportValidationException(int rowPosition, EntryImportFailureCause causeType, String message) {
        this(rowPosition, causeType, null, message);
    }

    public EntryImportValidationException(int rowPosition, EntryImportFailureCause causeType, String columnName, String message) {
        super(message);
        this.rowPosition = rowPosition;
        this.causeType = causeType;
        this.columnName = columnName;
    }

    public int getRowPosition() {
        return rowPosition;
    }

    public EntryImportFailureCause getCauseType() {
        return causeType;
    }

    public String getColumnName() {
        return columnName;
    }
}
