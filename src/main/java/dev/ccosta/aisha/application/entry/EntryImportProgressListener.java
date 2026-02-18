package dev.ccosta.aisha.application.entry;

public interface EntryImportProgressListener {

    void onStart(int totalRows);

    void onRowProcessed(int processedRows);
}
