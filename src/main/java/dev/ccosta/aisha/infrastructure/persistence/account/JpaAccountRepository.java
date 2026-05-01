package dev.ccosta.aisha.infrastructure.persistence.account;

import dev.ccosta.aisha.domain.account.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAccountRepository extends JpaRepository<Account, Long> {

    @EntityGraph(attributePaths = "openingBalance")
    List<Account> findAllByOrderByTitleAscIdAsc();

    @EntityGraph(attributePaths = "openingBalance")
    Page<Account> findAllByOrderByTitleAscIdAsc(Pageable pageable);

    @EntityGraph(attributePaths = "openingBalance")
    Optional<Account> findFirstByTitleIgnoreCaseOrderByIdAsc(String title);

    @Override
    @EntityGraph(attributePaths = "openingBalance")
    Optional<Account> findById(Long id);
}
