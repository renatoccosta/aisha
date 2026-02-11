package dev.ccosta.aisha.application.account;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.account.AccountRepository;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Account update(Long id, Account updatedData) {
        Account existing = findById(id);
        existing.setTitle(updatedData.getTitle());
        existing.setDescription(updatedData.getDescription());
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
