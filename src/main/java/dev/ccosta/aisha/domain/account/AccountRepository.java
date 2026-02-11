package dev.ccosta.aisha.domain.account;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    List<Account> findAllOrdered();

    Optional<Account> findById(Long id);

    Account save(Account account);

    void deleteById(Long id);

    void deleteByIds(Collection<Long> ids);
}
