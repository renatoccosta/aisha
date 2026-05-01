package dev.ccosta.aisha.application.entry.importing;

public record EntryCsvImportOptions(char delimiter, String datePattern, String amountPattern, boolean hasHeader) {
}
