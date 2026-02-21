package dev.ccosta.aisha.domain.account;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import dev.ccosta.aisha.domain.shared.PagedResult;

public interface AccountRepository {

    List<Account> findAllOrdered();

    PagedResult<Account> findPageOrdered(int page, int pageSize);

    Optional<Account> findById(Long id);

    Optional<Account> findByTitleIgnoreCase(String title);

    Account save(Account account);

    void deleteById(Long id);

    void deleteByIds(Collection<Long> ids);
}
