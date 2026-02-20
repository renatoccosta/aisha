package dev.ccosta.aisha.application.entry;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EntryCsvImportService {

    private static final int REQUIRED_COLUMN_COUNT = 6;
    private static final int NOTES_COLUMN_INDEX = 6;
    private static final int EXTERNAL_ID_COLUMN_INDEX = 7;
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final int MAX_NOTES_LENGTH = 1000;
    private static final int MAX_EXTERNAL_ID_LENGTH = 255;
    private static final Pattern GROUPED_DOT_INTEGER_PATTERN = Pattern.compile("^-?\\d{1,3}(?:\\.\\d{3})+$");
    private static final Pattern GROUPED_COMMA_INTEGER_PATTERN = Pattern.compile("^-?\\d{1,3}(?:,\\d{3})+$");
    private final EntryRepository entryRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public EntryCsvImportService(EntryRepository entryRepository, AccountService accountService, CategoryService categoryService) {
        this.entryRepository = entryRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
    }

    @Transactional
    public EntryImportSummary importCsv(byte[] fileContent, EntryCsvImportOptions options, EntryImportProgressListener progressListener) {
        long startedAtNanos = System.nanoTime();
        EntryCsvImportOptions safeOptions = options == null
            ? new EntryCsvImportOptions(',', "uuuu-MM-dd", "^-?(?:\\d{1,3}(?:\\.\\d{3})+|\\d+)(?:,\\d{1,2})?$", true)
            : options;
        EntryImportProgressListener safeProgressListener = progressListener == null ? new NoOpProgressListener() : progressListener;
        DateTimeFormatter dateFormatter = buildDateFormatter(safeOptions.datePattern());
        Pattern amountPattern = buildAmountPattern(safeOptions.amountPattern());

        List<CsvRow> rows = parseCsvRows(new String(fileContent, StandardCharsets.UTF_8), safeOptions.delimiter());
        int headerOffset = safeOptions.hasHeader() ? 1 : 0;
        int totalRows = Math.max(rows.size() - headerOffset, 0);
        safeProgressListener.onStart(totalRows);

        Map<String, Account> accountByTitleKey = new HashMap<>();
        for (Account account : accountService.listAllOrdered()) {
            accountByTitleKey.putIfAbsent(normalizeKey(account.getTitle()), account);
        }

        Map<String, Category> categoryByTitleKey = new HashMap<>();
        for (Category category : categoryService.listAllOrdered()) {
            categoryByTitleKey.putIfAbsent(normalizeKey(category.getTitle()), category);
        }

        int createdAccounts = 0;
        int createdCategories = 0;
        int imported = 0;
        int skippedDuplicates = 0;
        int processed = 0;

        Set<EntryFingerprint> fileFingerprints = new HashSet<>();
        Map<Long, LocalDate> earliestSettlementDateByAffectedAccountId = new HashMap<>();

        for (int rowIndex = headerOffset; rowIndex < rows.size(); rowIndex++) {
            CsvRow row = rows.get(rowIndex);
            int rowPosition = row.fileLine();
            processed++;

            CsvRecord record = toRecord(row, rowPosition, dateFormatter, amountPattern);
            ResolvedAccount resolvedAccount = resolveAccount(record.accountTitle(), accountByTitleKey);
            if (resolvedAccount.created()) {
                createdAccounts++;
            }
            Account account = resolvedAccount.account();

            ResolvedCategory resolvedCategory = resolveCategory(record.categoryTitle(), categoryByTitleKey);
            if (resolvedCategory.created()) {
                createdCategories++;
            }
            Category category = resolvedCategory.category();

            EntryFingerprint fingerprint = new EntryFingerprint(
                account.getId(),
                record.movementDate(),
                record.settlementDate(),
                record.description(),
                category.getId(),
                record.amount(),
                record.externalId()
            );

            if (!fileFingerprints.add(fingerprint) || entryRepository.existsDuplicate(
                fingerprint.accountId(),
                fingerprint.movementDate(),
                fingerprint.settlementDate(),
                fingerprint.description(),
                fingerprint.categoryId(),
                fingerprint.amount(),
                fingerprint.externalId()
            )) {
                skippedDuplicates++;
                safeProgressListener.onRowProcessed(processed);
                continue;
            }

            Entry entry = new Entry();
            entry.setAccount(account);
            entry.setMovementDate(record.movementDate());
            entry.setSettlementDate(record.settlementDate());
            entry.setDescription(record.description());
            entry.setCategory(category);
            entry.setAmount(record.amount());
            entry.setNotes(record.notes());
            entry.setExternalId(record.externalId());
            entryRepository.save(entry);

            if (
                !resolvedAccount.created()
                    && account.getInitialBalance() != null
                    && account.getInitialBalanceDate() != null
                    && !record.settlementDate().isAfter(account.getInitialBalanceDate())
            ) {
                earliestSettlementDateByAffectedAccountId.merge(
                    account.getId(),
                    record.settlementDate(),
                    (currentEarliest, candidate) -> candidate.isBefore(currentEarliest) ? candidate : currentEarliest
                );
            }

            imported++;
            safeProgressListener.onRowProcessed(processed);
        }

        accountService.adjustInitialBalanceForBackdatedEntries(earliestSettlementDateByAffectedAccountId);

        long durationMillis = (System.nanoTime() - startedAtNanos) / 1_000_000;

        return new EntryImportSummary(imported, skippedDuplicates, createdAccounts, createdCategories, durationMillis);
    }

    private CsvRecord toRecord(CsvRow row, int rowPosition, DateTimeFormatter dateFormatter, Pattern amountPattern) {
        if (row.columns().size() < REQUIRED_COLUMN_COUNT) {
            throw new EntryImportValidationException(
                rowPosition,
                EntryImportFailureCause.INVALID_FORMAT,
                null,
                "Invalid number of columns"
            );
        }

        String accountTitle = requireValue(row.columns().get(0), rowPosition, "account");
        validateLength(accountTitle, MAX_TITLE_LENGTH, rowPosition, "account");

        LocalDate movementDate = parseDate(row.columns().get(1), rowPosition, "movementDate", dateFormatter);
        LocalDate settlementDate = parseDate(row.columns().get(2), rowPosition, "settlementDate", dateFormatter);

        String description = requireValue(row.columns().get(3), rowPosition, "description");
        validateLength(description, MAX_DESCRIPTION_LENGTH, rowPosition, "description");

        String categoryTitle = requireValue(row.columns().get(4), rowPosition, "category");
        validateLength(categoryTitle, MAX_TITLE_LENGTH, rowPosition, "category");

        BigDecimal amount = parseAmount(row.columns().get(5), rowPosition, amountPattern);
        String notes = optionalValue(row.columns(), NOTES_COLUMN_INDEX);
        validateLength(notes, MAX_NOTES_LENGTH, rowPosition, "notes");
        String externalId = optionalValue(row.columns(), EXTERNAL_ID_COLUMN_INDEX);
        validateLength(externalId, MAX_EXTERNAL_ID_LENGTH, rowPosition, "externalId");

        return new CsvRecord(accountTitle, movementDate, settlementDate, description, categoryTitle, amount, notes, externalId);
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
        if (value == null) {
            return;
        }
        if (value.length() <= maxLength) {
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

    private LocalDate parseDate(String rawValue, int rowPosition, String fieldName, DateTimeFormatter dateFormatter) {
        String value = requireValue(rawValue, rowPosition, fieldName);
        try {
            return LocalDate.parse(value, dateFormatter);
        } catch (DateTimeException ex) {
            throw new EntryImportValidationException(
                rowPosition,
                EntryImportFailureCause.INVALID_FORMAT,
                fieldName,
                "Invalid date format: " + fieldName
            );
        }
    }

    private BigDecimal parseAmount(String rawValue, int rowPosition, Pattern amountPattern) {
        String value = requireValue(rawValue, rowPosition, "amount");
        if (!amountPattern.matcher(value).matches()) {
            throw new EntryImportValidationException(
                rowPosition,
                EntryImportFailureCause.INVALID_FORMAT,
                "amount",
                "Invalid amount format"
            );
        }

        String normalizedValue = normalizeAmountValue(value);
        try {
            return new BigDecimal(normalizedValue);
        } catch (NumberFormatException ex) {
            throw new EntryImportValidationException(
                rowPosition,
                EntryImportFailureCause.INVALID_FORMAT,
                "amount",
                "Invalid amount format"
            );
        }
    }

    private ResolvedAccount resolveAccount(String accountTitle, Map<String, Account> accountByTitleKey) {
        String titleKey = normalizeKey(accountTitle);
        Account account = accountByTitleKey.get(titleKey);
        if (account != null) {
            return new ResolvedAccount(account, false);
        }

        Account newAccount = new Account();
        newAccount.setTitle(accountTitle);
        newAccount.setDescription(null);
        newAccount.setInitialBalance(null);
        newAccount.setInitialBalanceDate(null);
        Account created = accountService.create(newAccount);
        accountByTitleKey.put(titleKey, created);
        return new ResolvedAccount(created, true);
    }

    private ResolvedCategory resolveCategory(String categoryTitle, Map<String, Category> categoryByTitleKey) {
        String titleKey = normalizeKey(categoryTitle);
        Category category = categoryByTitleKey.get(titleKey);
        if (category != null) {
            return new ResolvedCategory(category, false);
        }

        Category newCategory = new Category();
        newCategory.setTitle(categoryTitle);
        newCategory.setDescription(null);
        newCategory.setParent(null);
        Category created = categoryService.create(newCategory, null);
        categoryByTitleKey.put(titleKey, created);
        return new ResolvedCategory(created, true);
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT);
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

    private DateTimeFormatter buildDateFormatter(String datePattern) {
        String pattern = datePattern == null ? "" : datePattern.trim();
        if (!StringUtils.hasText(pattern)) {
            throw new IllegalArgumentException("Date pattern must not be blank");
        }

        try {
            return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ROOT)
                .withResolverStyle(ResolverStyle.STRICT);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid date format pattern", ex);
        }
    }

    private Pattern buildAmountPattern(String amountPattern) {
        String pattern = amountPattern == null ? "" : amountPattern.trim();
        if (!StringUtils.hasText(pattern)) {
            throw new IllegalArgumentException("Amount pattern must not be blank");
        }

        try {
            return Pattern.compile(pattern);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid amount format pattern", ex);
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

    private record CsvRecord(
        String accountTitle,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        String categoryTitle,
        BigDecimal amount,
        String notes,
        String externalId
    ) {
    }

    private record EntryFingerprint(
        Long accountId,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        Long categoryId,
        BigDecimal amount,
        String externalId
    ) {
    }

    private record ResolvedAccount(Account account, boolean created) {
    }

    private record ResolvedCategory(Category category, boolean created) {
    }

    private static final class NoOpProgressListener implements EntryImportProgressListener {
        @Override
        public void onStart(int totalRows) {
        }

        @Override
        public void onRowProcessed(int processedRows) {
        }
    }
}
