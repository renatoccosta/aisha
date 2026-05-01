package dev.ccosta.aisha.application.entry.importing.statement;

import dev.ccosta.aisha.application.entry.importing.EntryImportFailureCause;
import dev.ccosta.aisha.application.entry.importing.EntryImportValidationException;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BasicCsvStatementParser implements EntryStatementParser {

    private static final int REQUIRED_COLUMN_COUNT = 4;
    private static final int NOTES_COLUMN_INDEX = 4;
    private static final int EXTERNAL_ID_COLUMN_INDEX = 5;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final int MAX_NOTES_LENGTH = 1000;
    private static final int MAX_EXTERNAL_ID_LENGTH = 255;
    private static final Pattern GROUPED_DOT_INTEGER_PATTERN = Pattern.compile("^-?\\d{1,3}(?:\\.\\d{3})+$");
    private static final Pattern GROUPED_COMMA_INTEGER_PATTERN = Pattern.compile("^-?\\d{1,3}(?:,\\d{3})+$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^-?(?:\\d{1,3}(?:[.,]\\d{3})+|\\d+)(?:[.,]\\d{1,2})?$");
    private static final DateTimeFormatter ISO_DATE = new DateTimeFormatterBuilder()
        .appendPattern("uuuu-MM-dd")
        .toFormatter(Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter BR_DATE = new DateTimeFormatterBuilder()
        .appendPattern("dd/MM/uuuu")
        .toFormatter(Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT);

    @Override
    public EntryStatementFormat format() {
        return new EntryStatementFormat(
            "basic-csv",
            "entries.statementImport.format.basicCsv.label",
            "entries.statementImport.format.basicCsv.fileHelp"
        );
    }

    @Override
    public List<EntryStatementImportRecord> parse(byte[] fileContent) {
        String csvContent = new String(fileContent, java.nio.charset.StandardCharsets.UTF_8);
        char delimiter = resolveDelimiter(csvContent);
        List<CsvRow> rows = parseCsvRows(csvContent, delimiter);
        if (rows.isEmpty()) {
            return List.of();
        }

        int startIndex = hasHeader(rows.getFirst()) ? 1 : 0;
        List<EntryStatementImportRecord> records = new ArrayList<>();
        for (int rowIndex = startIndex; rowIndex < rows.size(); rowIndex++) {
            CsvRow row = rows.get(rowIndex);
            records.add(toRecord(row));
        }
        return records;
    }

    private EntryStatementImportRecord toRecord(CsvRow row) {
        if (row.columns().size() < REQUIRED_COLUMN_COUNT) {
            throw new EntryImportValidationException(
                row.fileLine(),
                EntryImportFailureCause.INVALID_FORMAT,
                null,
                "Invalid number of columns"
            );
        }

        LocalDate movementDate = parseDate(row.columns().get(0), row.fileLine(), "movementDate");
        LocalDate settlementDate = parseDate(row.columns().get(1), row.fileLine(), "settlementDate");
        String description = requireValue(row.columns().get(2), row.fileLine(), "description");
        validateLength(description, MAX_DESCRIPTION_LENGTH, row.fileLine(), "description");

        BigDecimal amount = parseAmount(row.columns().get(3), row.fileLine());
        String notes = optionalValue(row.columns(), NOTES_COLUMN_INDEX);
        validateLength(notes, MAX_NOTES_LENGTH, row.fileLine(), "notes");
        String externalId = optionalValue(row.columns(), EXTERNAL_ID_COLUMN_INDEX);
        validateLength(externalId, MAX_EXTERNAL_ID_LENGTH, row.fileLine(), "externalId");

        return new EntryStatementImportRecord(
            row.fileLine(),
            movementDate,
            settlementDate,
            description,
            amount,
            notes,
            externalId
        );
    }

    private boolean hasHeader(CsvRow firstRow) {
        if (firstRow.columns().isEmpty()) {
            return false;
        }

        String firstValue = firstRow.columns().getFirst();
        try {
            parseDate(firstValue, firstRow.fileLine(), "movementDate");
            return false;
        } catch (EntryImportValidationException ex) {
            return true;
        }
    }

    private LocalDate parseDate(String rawValue, int rowPosition, String fieldName) {
        String value = requireValue(rawValue, rowPosition, fieldName);
        try {
            return LocalDate.parse(value, ISO_DATE);
        } catch (DateTimeException ignored) {
            try {
                return LocalDate.parse(value, BR_DATE);
            } catch (DateTimeException ex) {
                throw new EntryImportValidationException(
                    rowPosition,
                    EntryImportFailureCause.INVALID_FORMAT,
                    fieldName,
                    "Invalid date format: " + fieldName
                );
            }
        }
    }

    private BigDecimal parseAmount(String rawValue, int rowPosition) {
        String value = requireValue(rawValue, rowPosition, "amount");
        if (!AMOUNT_PATTERN.matcher(value).matches()) {
            throw new EntryImportValidationException(
                rowPosition,
                EntryImportFailureCause.INVALID_FORMAT,
                "amount",
                "Invalid amount format"
            );
        }

        try {
            return new BigDecimal(normalizeAmountValue(value));
        } catch (NumberFormatException ex) {
            throw new EntryImportValidationException(
                rowPosition,
                EntryImportFailureCause.INVALID_FORMAT,
                "amount",
                "Invalid amount format"
            );
        }
    }

    private String normalizeAmountValue(String value) {
        String trimmedValue = value.trim();
        int lastCommaIndex = trimmedValue.lastIndexOf(',');
        int lastDotIndex = trimmedValue.lastIndexOf('.');

        if (lastCommaIndex >= 0 && lastDotIndex >= 0) {
            if (lastCommaIndex > lastDotIndex) {
                return trimmedValue.replace(".", "").replace(',', '.');
            }
            return trimmedValue.replace(",", "");
        }

        if (lastCommaIndex >= 0) {
            if (GROUPED_COMMA_INTEGER_PATTERN.matcher(trimmedValue).matches()) {
                return trimmedValue.replace(",", "");
            }
            return trimmedValue.replace(',', '.');
        }

        if (GROUPED_DOT_INTEGER_PATTERN.matcher(trimmedValue).matches()) {
            return trimmedValue.replace(".", "");
        }

        return trimmedValue;
    }

    private String requireValue(String value, int rowPosition, String fieldName) {
        String trimmedValue = value == null ? "" : value.trim();
        if (!StringUtils.hasText(trimmedValue)) {
            throw new EntryImportValidationException(
                rowPosition,
                EntryImportFailureCause.MISSING_REQUIRED_FIELD,
                fieldName,
                "Missing required field: " + fieldName
            );
        }
        return trimmedValue;
    }

    private void validateLength(String value, int maxLength, int rowPosition, String fieldName) {
        if (value == null || value.length() <= maxLength) {
            return;
        }

        throw new EntryImportValidationException(
            rowPosition,
            EntryImportFailureCause.INVALID_FORMAT,
            fieldName,
            "Invalid field size: " + fieldName
        );
    }

    private String optionalValue(List<String> columns, int index) {
        if (index >= columns.size()) {
            return null;
        }

        String value = columns.get(index);
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private char resolveDelimiter(String csvContent) {
        int commaCount = 0;
        int semicolonCount = 0;
        for (int i = 0; i < csvContent.length(); i++) {
            char currentChar = csvContent.charAt(i);
            if (currentChar == ',') {
                commaCount++;
            }
            if (currentChar == ';') {
                semicolonCount++;
            }

            if (currentChar == '\n') {
                break;
            }
        }

        return semicolonCount >= commaCount ? ';' : ',';
    }

    private List<CsvRow> parseCsvRows(String csvContent, char delimiter) {
        List<CsvRow> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        int fileLine = 1;

        for (int index = 0; index < csvContent.length(); index++) {
            char currentChar = csvContent.charAt(index);

            if (inQuotes) {
                if (currentChar == '"') {
                    if (index + 1 < csvContent.length() && csvContent.charAt(index + 1) == '"') {
                        currentField.append('"');
                        index++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    currentField.append(currentChar);
                }
                continue;
            }

            if (currentChar == '"') {
                inQuotes = true;
                continue;
            }

            if (currentChar == delimiter) {
                currentRow.add(currentField.toString());
                currentField.setLength(0);
                continue;
            }

            if (currentChar == '\n') {
                currentRow.add(currentField.toString());
                appendRow(rows, currentRow, fileLine);
                currentRow = new ArrayList<>();
                currentField.setLength(0);
                fileLine++;
                continue;
            }

            if (currentChar == '\r') {
                continue;
            }

            currentField.append(currentChar);
        }

        if (inQuotes) {
            throw new EntryImportValidationException(
                fileLine,
                EntryImportFailureCause.INVALID_FORMAT,
                null,
                "CSV has an unclosed quoted field"
            );
        }

        currentRow.add(currentField.toString());
        appendRow(rows, currentRow, fileLine);
        return rows;
    }

    private void appendRow(List<CsvRow> rows, List<String> row, int fileLine) {
        boolean isBlank = true;
        for (String value : row) {
            if (StringUtils.hasText(value)) {
                isBlank = false;
                break;
            }
        }

        if (!isBlank) {
            rows.add(new CsvRow(fileLine, row));
        }
    }

    private record CsvRow(int fileLine, List<String> columns) {
    }
}
