package dev.ccosta.aisha.application.entry.importing;

public interface EntryImportProgressListener {

    void onStart(int totalRows);

    void onRowProcessed(int processedRows);
}
