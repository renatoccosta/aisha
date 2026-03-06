package dev.ccosta.aisha.infrastructure.persistence.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaFederatedUserIdentityRepository extends JpaRepository<FederatedUserIdentity, Long> {

    Optional<FederatedUserIdentity> findByProviderAndSubject(String provider, String subject);
}
