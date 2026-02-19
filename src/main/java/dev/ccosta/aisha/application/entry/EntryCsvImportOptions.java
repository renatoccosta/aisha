package dev.ccosta.aisha.application.entry;

public record EntryCsvImportOptions(char delimiter, String datePattern, String amountPattern, boolean hasHeader) {
}
