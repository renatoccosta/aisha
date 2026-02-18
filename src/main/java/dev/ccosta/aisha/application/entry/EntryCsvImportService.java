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

    private static final int EXPECTED_COLUMN_COUNT = 6;
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^-?(?:\\d{1,3}(?:\\.\\d{3})+|\\d+),\\d{2}$");
    private static final List<String> EXPECTED_HEADER = List.of(
        "conta",
        "data de movimentacao",
        "data de liquidacao",
        "descricao",
        "categoria",
        "valor"
    );

    private final EntryRepository entryRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public EntryCsvImportService(EntryRepository entryRepository, AccountService accountService, CategoryService categoryService) {
        this.entryRepository = entryRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
    }

    @Transactional
    public EntryImportSummary importCsv(byte[] fileContent, EntryImportProgressListener progressListener) {
        long startedAtNanos = System.nanoTime();
        EntryImportProgressListener safeProgressListener = progressListener == null ? new NoOpProgressListener() : progressListener;

        List<CsvRow> rows = parseCsvRows(new String(fileContent, StandardCharsets.UTF_8));
        int headerOffset = hasHeader(rows) ? 1 : 0;
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

        for (int rowIndex = headerOffset; rowIndex < rows.size(); rowIndex++) {
            CsvRow row = rows.get(rowIndex);
            int rowPosition = rowIndex - headerOffset + 1;
            processed++;

            CsvRecord record = toRecord(row, rowPosition);
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
                record.amount()
            );

            if (!fileFingerprints.add(fingerprint) || entryRepository.existsDuplicate(
                fingerprint.accountId(),
                fingerprint.movementDate(),
                fingerprint.settlementDate(),
                fingerprint.description(),
                fingerprint.categoryId(),
                fingerprint.amount()
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
            entry.setNotes(null);
            entryRepository.save(entry);
            imported++;
            safeProgressListener.onRowProcessed(processed);
        }

        long durationMillis = (System.nanoTime() - startedAtNanos) / 1_000_000;

        return new EntryImportSummary(imported, skippedDuplicates, createdAccounts, createdCategories, durationMillis);
    }

    private CsvRecord toRecord(CsvRow row, int rowPosition) {
        if (row.columns().size() != EXPECTED_COLUMN_COUNT) {
            throw new EntryImportValidationException(
                rowPosition,
                EntryImportFailureCause.INVALID_FORMAT,
                "Invalid number of columns"
            );
        }

        String accountTitle = requireValue(row.columns().get(0), rowPosition, "account");
        validateLength(accountTitle, MAX_TITLE_LENGTH, rowPosition, "account");

        LocalDate movementDate = parseIsoDate(row.columns().get(1), rowPosition, "movementDate");
        LocalDate settlementDate = parseIsoDate(row.columns().get(2), rowPosition, "settlementDate");

        String description = requireValue(row.columns().get(3), rowPosition, "description");
        validateLength(description, MAX_DESCRIPTION_LENGTH, rowPosition, "description");

        String categoryTitle = requireValue(row.columns().get(4), rowPosition, "category");
        validateLength(categoryTitle, MAX_TITLE_LENGTH, rowPosition, "category");

        BigDecimal amount = parseAmount(row.columns().get(5), rowPosition);

        return new CsvRecord(accountTitle, movementDate, settlementDate, description, categoryTitle, amount);
    }

    private String requireValue(String value, int rowPosition, String fieldName) {
        String trimmedValue = value == null ? "" : value.trim();
        if (!StringUtils.hasText(trimmedValue)) {
            throw new EntryImportValidationException(
                rowPosition,
                EntryImportFailureCause.MISSING_REQUIRED_FIELD,
                "Missing required field: " + fieldName
            );
        }
        return trimmedValue;
    }

    private void validateLength(String value, int maxLength, int rowPosition, String fieldName) {
        if (value.length() <= maxLength) {
            return;
        }

        throw new EntryImportValidationException(
            rowPosition,
            EntryImportFailureCause.INVALID_FORMAT,
            "Invalid field size: " + fieldName
        );
    }

    private LocalDate parseIsoDate(String rawValue, int rowPosition, String fieldName) {
        String value = requireValue(rawValue, rowPosition, fieldName);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException ex) {
            throw new EntryImportValidationException(
                rowPosition,
                EntryImportFailureCause.INVALID_FORMAT,
                "Invalid ISO date format: " + fieldName
            );
        }
    }

    private BigDecimal parseAmount(String rawValue, int rowPosition) {
        String value = requireValue(rawValue, rowPosition, "amount");
        if (!AMOUNT_PATTERN.matcher(value).matches()) {
            throw new EntryImportValidationException(
                rowPosition,
                EntryImportFailureCause.INVALID_FORMAT,
                "Invalid amount format"
            );
        }

        String normalizedValue = value.replace(".", "").replace(',', '.');
        try {
            return new BigDecimal(normalizedValue);
        } catch (NumberFormatException ex) {
            throw new EntryImportValidationException(
                rowPosition,
                EntryImportFailureCause.INVALID_FORMAT,
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

    private boolean hasHeader(List<CsvRow> rows) {
        if (rows.isEmpty()) {
            return false;
        }

        CsvRow firstRow = rows.getFirst();
        if (firstRow.columns().size() != EXPECTED_COLUMN_COUNT) {
            return false;
        }

        for (int index = 0; index < EXPECTED_HEADER.size(); index++) {
            if (!normalizeKey(firstRow.columns().get(index)).equals(EXPECTED_HEADER.get(index))) {
                return false;
            }
        }

        return true;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT);
    }

    private List<CsvRow> parseCsvRows(String csvContent) {
        char delimiter = resolveDelimiter(csvContent);
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
                "CSV has an unclosed quoted field"
            );
        }

        currentRow.add(currentField.toString());
        appendRow(rows, currentRow, fileLine);
        return rows;
    }

    private char resolveDelimiter(String csvContent) {
        int semicolonCount = 0;
        int commaCount = 0;
        boolean inQuotes = false;

        for (int index = 0; index < csvContent.length(); index++) {
            char currentChar = csvContent.charAt(index);
            if (currentChar == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (inQuotes) {
                continue;
            }
            if (currentChar == ';') {
                semicolonCount++;
            } else if (currentChar == ',') {
                commaCount++;
            } else if (currentChar == '\n') {
                break;
            }
        }

        return semicolonCount > commaCount ? ';' : ',';
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
        BigDecimal amount
    ) {
    }

    private record EntryFingerprint(
        Long accountId,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        Long categoryId,
        BigDecimal amount
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
