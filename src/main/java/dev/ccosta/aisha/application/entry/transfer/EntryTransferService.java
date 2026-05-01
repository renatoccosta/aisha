package dev.ccosta.aisha.application.entry.transfer;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.entry.EntryNotFoundException;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.categorization.EntryCategorySuggestionStatus;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.entry.EntrySource;
import dev.ccosta.aisha.domain.entry.transfer.EntryTransfer;
import dev.ccosta.aisha.domain.entry.transfer.EntryTransferRepository;
import dev.ccosta.aisha.domain.entry.EntryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Coordinates the lifecycle of transfer entries, which must always exist as a linked debit/credit pair.
 */
@Service
public class EntryTransferService {

    private final EntryRepository entryRepository;
    private final EntryTransferRepository entryTransferRepository;
    private final AccountService accountService;

    public EntryTransferService(
        EntryRepository entryRepository,
        EntryTransferRepository entryTransferRepository,
        AccountService accountService
    ) {
        this.entryRepository = entryRepository;
        this.entryTransferRepository = entryTransferRepository;
        this.accountService = accountService;
    }

    /**
     * Creates a new transfer by generating both account-side entries and linking them atomically.
     */
    @Transactional
    public EntryTransfer createTransfer(EntryTransferCreationRequest request) {
        validateTransferCreationRequest(request);
        Account originAccount = accountService.findById(request.originAccountId());
        Account destinationAccount = accountService.findById(request.destinationAccountId());
        accountService.validateEntrySettlementDateAgainstAccountDeactivation(originAccount.getId(), request.settlementDate());
        accountService.validateEntrySettlementDateAgainstAccountDeactivation(destinationAccount.getId(), request.settlementDate());

        Entry originEntry = buildTransferEntry(
            originAccount,
            request.movementDate(),
            request.settlementDate(),
            request.description(),
            request.amount().abs().negate(),
            request.notes()
        );
        Entry destinationEntry = buildTransferEntry(
            destinationAccount,
            request.movementDate(),
            request.settlementDate(),
            request.description(),
            request.amount().abs(),
            request.notes()
        );

        Entry savedOrigin = entryRepository.save(originEntry);
        Entry savedDestination = entryRepository.save(destinationEntry);
        accountService.adjustInitialBalanceForBackdatedEntry(originAccount.getId(), savedOrigin.getSettlementDate());
        accountService.adjustInitialBalanceForBackdatedEntry(destinationAccount.getId(), savedDestination.getSettlementDate());

        return entryTransferRepository.save(newTransfer(savedOrigin, savedDestination, request.notes()));
    }

    /**
     * Updates both entries that compose an existing transfer, preserving the debit/credit pairing.
     */
    @Transactional
    public EntryTransfer updateTransferByEntryId(Long entryId, EntryTransferCreationRequest request) {
        validateTransferCreationRequest(request);
        EntryTransfer entryTransfer = findTransferByEntryId(entryId)
            .orElseThrow(() -> new IllegalArgumentException("Entry is not part of a transfer"));

        Account originAccount = accountService.findById(request.originAccountId());
        Account destinationAccount = accountService.findById(request.destinationAccountId());
        accountService.validateEntrySettlementDateAgainstAccountDeactivation(originAccount.getId(), request.settlementDate());
        accountService.validateEntrySettlementDateAgainstAccountDeactivation(destinationAccount.getId(), request.settlementDate());

        Entry originEntry = entryTransfer.getOriginEntry();
        Entry destinationEntry = entryTransfer.getDestinationEntry();

        updateTransferEntry(
            originEntry,
            originAccount,
            request.movementDate(),
            request.settlementDate(),
            request.description(),
            request.amount().abs().negate(),
            request.notes()
        );
        updateTransferEntry(
            destinationEntry,
            destinationAccount,
            request.movementDate(),
            request.settlementDate(),
            request.description(),
            request.amount().abs(),
            request.notes()
        );

        entryRepository.save(originEntry);
        entryRepository.save(destinationEntry);
        entryTransfer.setNotes(request.notes());
        accountService.adjustInitialBalanceForBackdatedEntry(originAccount.getId(), request.settlementDate());
        accountService.adjustInitialBalanceForBackdatedEntry(destinationAccount.getId(), request.settlementDate());
        return entryTransferRepository.save(entryTransfer);
    }

    /**
     * Converts two existing regular entries into a transfer after validating the pair invariants.
     */
    @Transactional
    public EntryTransfer linkExistingEntries(Long originEntryId, Long destinationEntryId) {
        Entry firstEntry = findRegularEntry(originEntryId);
        Entry secondEntry = findRegularEntry(destinationEntryId);
        validateEntriesCanBecomeTransfer(firstEntry, secondEntry);

        Entry originEntry = firstEntry.getAmount().signum() < 0 ? firstEntry : secondEntry;
        Entry destinationEntry = firstEntry.getAmount().signum() > 0 ? firstEntry : secondEntry;

        convertExistingEntryToTransfer(originEntry);
        convertExistingEntryToTransfer(destinationEntry);
        entryRepository.save(originEntry);
        entryRepository.save(destinationEntry);

        return entryTransferRepository.save(newTransfer(originEntry, destinationEntry, null));
    }

    /**
     * Creates the missing side of a transfer from an existing regular entry and converts the original entry.
     */
    @Transactional
    public EntryTransfer createCounterpartFromEntry(Long sourceEntryId, EntryTransferCounterpartRequest request) {
        Entry sourceEntry = findRegularEntry(sourceEntryId);
        validateCounterpartRequest(sourceEntry, request);

        Account counterpartAccount = accountService.findById(request.counterpartAccountId());
        accountService.validateEntrySettlementDateAgainstAccountDeactivation(sourceEntry.getAccount().getId(), request.settlementDate());
        accountService.validateEntrySettlementDateAgainstAccountDeactivation(counterpartAccount.getId(), request.settlementDate());

        convertExistingEntryToTransfer(sourceEntry);
        sourceEntry.setMovementDate(request.movementDate());
        sourceEntry.setSettlementDate(request.settlementDate());
        sourceEntry.setDescription(normalizeDescription(request.description()));
        sourceEntry.setNotes(request.notes());
        entryRepository.save(sourceEntry);
        accountService.adjustInitialBalanceForBackdatedEntry(sourceEntry.getAccount().getId(), sourceEntry.getSettlementDate());

        Entry counterpartEntry = buildTransferEntry(
            counterpartAccount,
            request.movementDate(),
            request.settlementDate(),
            request.description(),
            sourceEntry.getAmount().negate(),
            request.notes()
        );
        Entry savedCounterpart = entryRepository.save(counterpartEntry);
        accountService.adjustInitialBalanceForBackdatedEntry(counterpartAccount.getId(), savedCounterpart.getSettlementDate());

        Entry originEntry = sourceEntry.getAmount().signum() < 0 ? sourceEntry : savedCounterpart;
        Entry destinationEntry = sourceEntry.getAmount().signum() < 0 ? savedCounterpart : sourceEntry;
        return entryTransferRepository.save(newTransfer(originEntry, destinationEntry, request.notes()));
    }

    /**
     * Removes the transfer link and converts both entries back to regular uncategorized entries.
     */
    @Transactional
    public void unlinkTransferByEntryId(Long entryId) {
        EntryTransfer entryTransfer = findTransferByEntryId(entryId)
            .orElseThrow(() -> new IllegalArgumentException("Entry is not part of a transfer"));

        entryTransferRepository.delete(entryTransfer);

        Entry originEntry = entryTransfer.getOriginEntry();
        Entry destinationEntry = entryTransfer.getDestinationEntry();
        convertTransferEntryToRegular(originEntry);
        convertTransferEntryToRegular(destinationEntry);
        entryRepository.save(originEntry);
        entryRepository.save(destinationEntry);
    }

    @Transactional(readOnly = true)
    public Optional<EntryTransferView> findTransferViewByEntryId(Long entryId) {
        return findTransferByEntryId(entryId)
            .map(transfer -> {
                boolean originSide = transfer.getOriginEntry().getId().equals(entryId);
                Entry counterpart = originSide ? transfer.getDestinationEntry() : transfer.getOriginEntry();
                return new EntryTransferView(
                    transfer.getId(),
                    counterpart.getId(),
                    counterpart.getAccount().getId(),
                    counterpart.getAccount().getTitle(),
                    originSide
                );
            });
    }

    @Transactional(readOnly = true)
    public EntryTransfer findTransferByAnyEntryId(Long entryId) {
        return findTransferByEntryId(entryId)
            .orElseThrow(() -> new IllegalArgumentException("Entry is not part of a transfer"));
    }

    /**
     * Validates whether two existing regular entries can be linked as a transfer without mutating them.
     */
    @Transactional(readOnly = true)
    public void validateExistingEntriesCanBecomeTransfer(Long firstEntryId, Long secondEntryId) {
        Entry firstEntry = findRegularEntry(firstEntryId);
        Entry secondEntry = findRegularEntry(secondEntryId);
        validateEntriesCanBecomeTransfer(firstEntry, secondEntry);
    }

    private Entry findRegularEntry(Long entryId) {
        Entry entry = entryRepository.findById(entryId)
            .orElseThrow(() -> new EntryNotFoundException(entryId));
        if (entryTransferRepository.existsByEntryId(entryId) || entry.isTransfer()) {
            throw new IllegalArgumentException("Entry is already part of a transfer");
        }
        return entry;
    }

    private Optional<EntryTransfer> findTransferByEntryId(Long entryId) {
        return entryTransferRepository.findByEntryId(entryId);
    }

    private void validateTransferCreationRequest(EntryTransferCreationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Transfer request is required");
        }
        if (request.originAccountId() == null || request.destinationAccountId() == null) {
            throw new IllegalArgumentException("Both accounts are required");
        }
        if (request.originAccountId().equals(request.destinationAccountId())) {
            throw new IllegalArgumentException("Origin and destination accounts must be different");
        }
        validateCommonTransferFields(request.movementDate(), request.settlementDate(), request.description(), request.amount());
    }

    private void validateCounterpartRequest(Entry sourceEntry, EntryTransferCounterpartRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Transfer counterpart request is required");
        }
        if (request.counterpartAccountId() == null) {
            throw new IllegalArgumentException("Counterpart account is required");
        }
        if (sourceEntry.getAccount().getId().equals(request.counterpartAccountId())) {
            throw new IllegalArgumentException("Counterpart account must be different from the source account");
        }
        validateCommonTransferFields(
            request.movementDate(),
            request.settlementDate(),
            request.description(),
            sourceEntry.getAmount().abs()
        );
    }

    private void validateEntriesCanBecomeTransfer(Entry first, Entry second) {
        if (first.getId().equals(second.getId())) {
            throw new IllegalArgumentException("A transfer requires two distinct entries");
        }
        if (first.getAccount() == null || second.getAccount() == null) {
            throw new IllegalArgumentException("Both entries must belong to an account");
        }
        if (first.getAccount().getId().equals(second.getAccount().getId())) {
            throw new IllegalArgumentException("Transfer entries must belong to different accounts");
        }
        if (first.getAmount() == null || second.getAmount() == null || first.getAmount().signum() == 0 || second.getAmount().signum() == 0) {
            throw new IllegalArgumentException("Transfer entries must have non-zero amounts");
        }
        if (first.getAmount().signum() == second.getAmount().signum()) {
            throw new IllegalArgumentException("Transfer entries must have opposite signs");
        }
        if (first.getAmount().abs().compareTo(second.getAmount().abs()) != 0) {
            throw new IllegalArgumentException("Transfer entries must have the same absolute amount");
        }
        if (!first.getMovementDate().equals(second.getMovementDate()) || !first.getSettlementDate().equals(second.getSettlementDate())) {
            throw new IllegalArgumentException("Transfer entries must have matching movement and settlement dates");
        }
    }

    private void validateCommonTransferFields(LocalDate movementDate, LocalDate settlementDate, String description, BigDecimal amount) {
        if (movementDate == null || settlementDate == null) {
            throw new IllegalArgumentException("Movement and settlement dates are required");
        }
        if (!StringUtils.hasText(description)) {
            throw new IllegalArgumentException("Description is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }
    }

    private Entry buildTransferEntry(
        Account account,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        BigDecimal amount,
        String notes
    ) {
        Entry entry = new Entry();
        entry.setAccount(account);
        entry.setMovementDate(movementDate);
        entry.setSettlementDate(settlementDate);
        entry.setDescription(normalizeDescription(description));
        entry.setNotes(notes);
        entry.setAmount(amount);
        entry.setEntrySource(EntrySource.MANUAL);
        entry.setRegistrationDate(LocalDate.now());
        entry.setEntryType(EntryType.TRANSFER);
        clearCategoryState(entry);
        return entry;
    }

    private void updateTransferEntry(
        Entry entry,
        Account account,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        BigDecimal amount,
        String notes
    ) {
        entry.setAccount(account);
        entry.setMovementDate(movementDate);
        entry.setSettlementDate(settlementDate);
        entry.setDescription(normalizeDescription(description));
        entry.setNotes(notes);
        entry.setAmount(amount);
        entry.setEntryType(EntryType.TRANSFER);
        clearCategoryState(entry);
    }

    private EntryTransfer newTransfer(Entry originEntry, Entry destinationEntry, String notes) {
        EntryTransfer entryTransfer = new EntryTransfer();
        entryTransfer.setOriginEntry(originEntry);
        entryTransfer.setDestinationEntry(destinationEntry);
        entryTransfer.setCreatedAt(LocalDateTime.now());
        entryTransfer.setNotes(notes);
        return entryTransfer;
    }

    private void convertExistingEntryToTransfer(Entry entry) {
        entry.setEntryType(EntryType.TRANSFER);
        clearCategoryState(entry);
    }

    private void convertTransferEntryToRegular(Entry entry) {
        entry.setEntryType(EntryType.REGULAR);
        clearCategoryState(entry);
    }

    private void clearCategoryState(Entry entry) {
        entry.setCategory(null);
        entry.setSuggestedCategory(null);
        entry.setCategorySuggestionConfidence(null);
        entry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.NONE);
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }
}
