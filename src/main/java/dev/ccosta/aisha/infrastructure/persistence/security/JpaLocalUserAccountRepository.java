package dev.ccosta.aisha.infrastructure.persistence.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaLocalUserAccountRepository extends JpaRepository<LocalUserAccount, Long> {

    Optional<LocalUserAccount> findByUsername(String username);
}
