package dev.ccosta.aisha.application.account;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.account.AccountRepository;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
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
    public Account findById(Long id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional
    public Account create(Account account) {
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
                return accountRepository.save(account);
            });
    }

    @Transactional
    public Account update(Long id, Account updatedData) {
        Account existing = findById(id);
        existing.setTitle(updatedData.getTitle());
        existing.setDescription(updatedData.getDescription());
        existing.setInitialBalance(updatedData.getInitialBalance());
        existing.setInitialBalanceDate(updatedData.getInitialBalanceDate());
        return accountRepository.save(existing);
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

    private void ensureAccountIsNotInUse(Long id) {
        if (entryRepository.existsByAccountId(id)) {
            throw new AccountInUseException(id);
        }
    }
}
