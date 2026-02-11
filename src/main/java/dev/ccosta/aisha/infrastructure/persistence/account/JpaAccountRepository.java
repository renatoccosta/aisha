package dev.ccosta.aisha.infrastructure.persistence.account;

import dev.ccosta.aisha.domain.account.Account;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByOrderByTitleAscIdAsc();
}
