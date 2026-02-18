package dev.ccosta.aisha.infrastructure.persistence.security;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface JpaLocalUserAccountRepository extends JpaRepository<LocalUserAccount, Long> {

    Optional<LocalUserAccount> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<LocalUserAccount> findAllByOrderByUsernameAsc();

    @Modifying
    @Query("delete from LocalUserAccount account where account.id in (:ids)")
    void deleteByIds(Collection<Long> ids);
}
