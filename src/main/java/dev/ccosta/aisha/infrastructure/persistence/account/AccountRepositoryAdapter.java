package dev.ccosta.aisha.infrastructure.persistence.account;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.account.AccountRepository;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public PagedResult<Account> findPageOrdered(int page, int pageSize) {
        Page<Account> result = jpaAccountRepository.findAllByOrderByTitleAscIdAsc(PageRequest.of(page, pageSize));
        return new PagedResult<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public Optional<Account> findById(Long id) {
        return jpaAccountRepository.findById(id);
    }

    @Override
    public Optional<Account> findByTitleIgnoreCase(String title) {
        return jpaAccountRepository.findFirstByTitleIgnoreCaseOrderByIdAsc(title);
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
