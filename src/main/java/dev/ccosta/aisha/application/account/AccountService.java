package dev.ccosta.aisha.application.account;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.account.AccountRepository;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.application.entry.EntrySettlementAfterAccountDeactivationException;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final EntryRepository entryRepository;

    public AccountService(AccountRepository accountRepository, EntryRepository entryRepository) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional(readOnly = true)
    public List<Account> listAllOrdered() {
        return accountRepository.findAllOrdered();
    }

    @Transactional(readOnly = true)
    public List<Account> listAllActiveOrdered() {
        return accountRepository.findAllOrdered()
            .stream()
            .filter(account -> account.getDeactivationDate() == null)
            .toList();
    }

    @Transactional(readOnly = true)
    public PagedResult<Account> listPageOrdered(int page, int pageSize) {
        return accountRepository.findPageOrdered(page, pageSize);
    }

    @Transactional(readOnly = true)
    public List<Account> listAvailableForEntryForm(Long selectedAccountId) {
        return accountRepository.findAllOrdered()
            .stream()
            .filter(account -> account.getDeactivationDate() == null || (selectedAccountId != null && selectedAccountId.equals(account.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Account> listVisibleForEntryFilter(LocalDate globalStartDate) {
        if (globalStartDate == null) {
            return listAllOrdered();
        }

        return accountRepository.findAllOrdered()
            .stream()
            .filter(account -> account.getDeactivationDate() == null || !account.getDeactivationDate().isBefore(globalStartDate))
            .toList();
    }

    @Transactional(readOnly = true)
    public Account findById(Long id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional
    public Account create(Account account) {
        validateDeactivationDate(account.getDeactivationDate(), null);
        return accountRepository.save(account);
    }

    @Transactional
    public Account findOrCreateByTitle(String rawTitle) {
        String title = rawTitle == null ? "" : rawTitle.trim();
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("Account title must not be blank");
        }

        return accountRepository.findByTitleIgnoreCase(title)
            .orElseGet(() -> {
                Account account = new Account();
                account.setTitle(title);
                account.setDescription(null);
                account.setInitialBalance(null);
                account.setInitialBalanceDate(null);
                account.setDeactivationDate(null);
                return accountRepository.save(account);
            });
    }

    @Transactional
    public Account update(Long id, Account updatedData) {
        Account existing = findById(id);
        validateDeactivationDate(updatedData.getDeactivationDate(), id);
        existing.setTitle(updatedData.getTitle());
        existing.setDescription(updatedData.getDescription());
        existing.setInitialBalance(updatedData.getInitialBalance());
        existing.setInitialBalanceDate(updatedData.getInitialBalanceDate());
        existing.setDeactivationDate(updatedData.getDeactivationDate());
        return accountRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public void validateEntrySettlementDateAgainstAccountDeactivation(Long accountId, LocalDate settlementDate) {
        if (accountId == null || settlementDate == null) {
            return;
        }

        Account account = findById(accountId);
        LocalDate deactivationDate = account.getDeactivationDate();
        if (deactivationDate == null || !settlementDate.isAfter(deactivationDate)) {
            return;
        }

        throw new EntrySettlementAfterAccountDeactivationException(settlementDate, deactivationDate);
    }

    @Transactional
    public void deleteById(Long id) {
        findById(id);
        ensureAccountIsNotInUse(id);
        accountRepository.deleteById(id);
    }

    @Transactional
    public void bulkDelete(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(ids);
        for (Long id : uniqueIds) {
            findById(id);
            ensureAccountIsNotInUse(id);
        }

        accountRepository.deleteByIds(uniqueIds);
    }

    @Transactional
    public void adjustInitialBalanceForBackdatedEntry(Long accountId, LocalDate settlementDate) {
        if (accountId == null || settlementDate == null) {
            return;
        }

        Account account = findById(accountId);
        applyBackdatedBalanceAdjustment(accountId, account, settlementDate);
    }

    @Transactional
    public void adjustInitialBalanceForBackdatedEntries(Map<Long, LocalDate> earliestSettlementDateByAccountId) {
        if (earliestSettlementDateByAccountId == null || earliestSettlementDateByAccountId.isEmpty()) {
            return;
        }

        for (Map.Entry<Long, LocalDate> entry : earliestSettlementDateByAccountId.entrySet()) {
            Long accountId = entry.getKey();
            LocalDate earliestSettlementDate = entry.getValue();
            if (accountId == null || earliestSettlementDate == null) {
                continue;
            }

            Account account = findById(accountId);
            applyBackdatedBalanceAdjustment(accountId, account, earliestSettlementDate);
        }
    }

    private void ensureAccountIsNotInUse(Long id) {
        if (entryRepository.existsByAccountId(id)) {
            throw new AccountInUseException(id);
        }
    }

    private void applyBackdatedBalanceAdjustment(Long accountId, Account account, LocalDate earliestSettlementDate) {
        BigDecimal initialBalance = account.getInitialBalance();
        LocalDate initialBalanceDate = account.getInitialBalanceDate();
        if (initialBalance == null || initialBalanceDate == null) {
            return;
        }
        if (earliestSettlementDate.isAfter(initialBalanceDate)) {
            return;
        }

        BigDecimal amountToRollback = entryRepository.sumAmountByAccountIdAndSettlementDateBetween(
            accountId,
            earliestSettlementDate,
            initialBalanceDate
        );
        LocalDate adjustedInitialBalanceDate = earliestSettlementDate.minusDays(1);
        account.setInitialBalance(initialBalance.subtract(amountToRollback));
        account.setInitialBalanceDate(adjustedInitialBalanceDate);
        accountRepository.save(account);
    }

    private void validateDeactivationDate(LocalDate deactivationDate, Long accountId) {
        if (deactivationDate == null || accountId == null) {
            return;
        }

        LocalDate latestSettlementDate = entryRepository.findLatestSettlementDateByAccountId(accountId).orElse(null);
        if (latestSettlementDate == null || !deactivationDate.isBefore(latestSettlementDate)) {
            return;
        }

        throw new AccountInvalidDeactivationDateException(deactivationDate, latestSettlementDate);
    }
}
