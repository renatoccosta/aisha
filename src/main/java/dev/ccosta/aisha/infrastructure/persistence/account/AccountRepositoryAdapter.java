package dev.ccosta.aisha.infrastructure.persistence.account;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.account.AccountRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepositoryAdapter implements AccountRepository {

    private final JpaAccountRepository jpaAccountRepository;

    public AccountRepositoryAdapter(JpaAccountRepository jpaAccountRepository) {
        this.jpaAccountRepository = jpaAccountRepository;
    }

    @Override
    public List<Account> findAllOrdered() {
        return jpaAccountRepository.findAllByOrderByTitleAscIdAsc();
    }

    @Override
    public Optional<Account> findById(Long id) {
        return jpaAccountRepository.findById(id);
    }

    @Override
    public Account save(Account account) {
        return jpaAccountRepository.save(account);
    }

    @Override
    public void deleteById(Long id) {
        jpaAccountRepository.deleteById(id);
    }

    @Override
    public void deleteByIds(Collection<Long> ids) {
        jpaAccountRepository.deleteAllByIdInBatch(ids);
    }
}
