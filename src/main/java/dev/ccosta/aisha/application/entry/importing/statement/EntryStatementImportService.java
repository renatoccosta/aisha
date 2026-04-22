package dev.ccosta.aisha.application.entry.importing.statement;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestion;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestionRequest;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestionService;
import dev.ccosta.aisha.application.entry.importing.EntryImportProgressListener;
import dev.ccosta.aisha.application.entry.importing.EntryImportSummary;
import dev.ccosta.aisha.application.entry.importing.EntryImportValidationException;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.categorization.EntryCategorySuggestionStatus;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.entry.EntrySource;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntryStatementImportService {

    private final EntryRepository entryRepository;
    private final AccountService accountService;
    private final EntryStatementParserRegistry parserRegistry;
    private final EntryCategorySuggestionService entryCategorySuggestionService;

    public EntryStatementImportService(
        EntryRepository entryRepository,
        AccountService accountService,
        EntryStatementParserRegistry parserRegistry,
        EntryCategorySuggestionService entryCategorySuggestionService
    ) {
        this.entryRepository = entryRepository;
        this.accountService = accountService;
        this.parserRegistry = parserRegistry;
        this.entryCategorySuggestionService = entryCategorySuggestionService;
    }

    @Transactional(readOnly = true)
    public List<EntryStatementFormat> listAvailableFormats() {
        return parserRegistry.listFormats();
    }

    @Transactional
    public EntryImportSummary importStatement(
        Long accountId,
        String formatId,
        byte[] fileContent,
        EntryImportProgressListener progressListener
    ) {
        long startedAtNanos = System.nanoTime();
        EntryImportProgressListener safeProgressListener = progressListener == null ? new NoOpProgressListener() : progressListener;
        Account account = resolveActiveAccount(accountId);
        EntryStatementParser parser = parserRegistry.resolve(formatId);

        List<EntryStatementImportRecord> records = parser.parse(fileContent);
        safeProgressListener.onStart(records.size());

        int imported = 0;
        int skippedDuplicates = 0;
        int processed = 0;

        Set<EntryFingerprint> fileFingerprints = new HashSet<>();
        LocalDate earliestSettlementDate = null;
        for (EntryStatementImportRecord record : records) {
            processed++;
            validateSettlementDate(account, record);

            EntryFingerprint fingerprint = new EntryFingerprint(
                account.getId(),
                record.movementDate(),
                record.settlementDate(),
                record.description(),
                record.amount(),
                record.externalId()
            );

            if (
                !fileFingerprints.add(fingerprint)
                    || entryRepository.existsDuplicate(
                        fingerprint.accountId(),
                        fingerprint.movementDate(),
                        fingerprint.settlementDate(),
                        fingerprint.description(),
                        null,
                        fingerprint.amount(),
                        fingerprint.externalId()
                    )
                    || entryRepository.existsDuplicateIgnoringCategory(
                        fingerprint.accountId(),
                        fingerprint.movementDate(),
                        fingerprint.settlementDate(),
                        fingerprint.description(),
                        fingerprint.amount(),
                        fingerprint.externalId()
                    )
            ) {
                skippedDuplicates++;
                safeProgressListener.onRowProcessed(processed);
                continue;
            }

            Entry entry = new Entry();
            entry.setAccount(account);
            entry.setMovementDate(record.movementDate());
            entry.setSettlementDate(record.settlementDate());
            entry.setDescription(record.description());
            entry.setAmount(record.amount());
            entry.setNotes(record.notes());
            entry.setExternalId(record.externalId());
            entry.setEntrySource(EntrySource.IMPORT);
            entry.setRegistrationDate(LocalDate.now());
            applyCategorySuggestion(entry, account, record);
            entryRepository.save(entry);
            imported++;

            if (
                account.getInitialBalance() != null
                    && account.getInitialBalanceDate() != null
                    && !record.settlementDate().isAfter(account.getInitialBalanceDate())
            ) {
                if (earliestSettlementDate == null || record.settlementDate().isBefore(earliestSettlementDate)) {
                    earliestSettlementDate = record.settlementDate();
                }
            }

            safeProgressListener.onRowProcessed(processed);
        }

        if (earliestSettlementDate != null) {
            accountService.adjustInitialBalanceForBackdatedEntries(Map.of(account.getId(), earliestSettlementDate));
        }

        long durationMillis = (System.nanoTime() - startedAtNanos) / 1_000_000;
        return new EntryImportSummary(imported, skippedDuplicates, 0, 0, durationMillis);
    }

    private Account resolveActiveAccount(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account must be informed");
        }

        Account account = accountService.findById(accountId);
        if (account.getDeactivationDate() != null) {
            throw new IllegalArgumentException("Selected account must be active");
        }

        return account;
    }

    private void validateSettlementDate(Account account, EntryStatementImportRecord record) {
        LocalDate deactivationDate = account.getDeactivationDate();
        if (deactivationDate == null || !record.settlementDate().isAfter(deactivationDate)) {
            return;
        }

        throw new EntryImportValidationException(
            record.rowPosition(),
            dev.ccosta.aisha.application.entry.importing.EntryImportFailureCause.INVALID_FORMAT,
            "settlementDate",
            "Settlement date must not be after account deactivation date"
        );
    }

    private void applyCategorySuggestion(Entry entry, Account account, EntryStatementImportRecord record) {
        entryCategorySuggestionService.suggest(new EntryCategorySuggestionRequest(account.getId(), record.description(), record.amount()))
            .ifPresentOrElse(
                suggestion -> applySuggestedCategory(entry, suggestion),
                () -> entry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.NONE)
            );
    }

    private void applySuggestedCategory(Entry entry, EntryCategorySuggestion suggestion) {
        entry.setCategory(suggestion.category());
        entry.setSuggestedCategory(suggestion.category());
        entry.setCategorySuggestionConfidence(suggestion.confidence());
        entry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.PENDING);
    }

    private record EntryFingerprint(
        Long accountId,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        java.math.BigDecimal amount,
        String externalId
    ) {
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
