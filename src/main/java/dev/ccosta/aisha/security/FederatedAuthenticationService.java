package dev.ccosta.aisha.security;

import dev.ccosta.aisha.infrastructure.persistence.security.FederatedUserIdentity;
import dev.ccosta.aisha.infrastructure.persistence.security.JpaFederatedUserIdentityRepository;
import dev.ccosta.aisha.infrastructure.persistence.security.JpaLocalUserAccountRepository;
import dev.ccosta.aisha.infrastructure.persistence.security.LocalUserAccount;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Coordinates local account resolution and federated identity persistence for OAuth2/OIDC logins.
 */
@Service
public class FederatedAuthenticationService {

    private final JpaLocalUserAccountRepository localUserAccountRepository;
    private final JpaFederatedUserIdentityRepository federatedUserIdentityRepository;
    private final PasswordEncoder passwordEncoder;

    public FederatedAuthenticationService(
        JpaLocalUserAccountRepository localUserAccountRepository,
        JpaFederatedUserIdentityRepository federatedUserIdentityRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.localUserAccountRepository = localUserAccountRepository;
        this.federatedUserIdentityRepository = federatedUserIdentityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Resolves the local account for a federated login. If needed, creates a new local account and persists
     * the external identity link.
     */
    @Transactional
    public LocalUserAccount resolveOrCreateLocalAccount(String provider, String subject, String email) {
        FederatedUserIdentity existingIdentity = federatedUserIdentityRepository.findByProviderAndSubject(provider, subject)
            .orElse(null);
        if (existingIdentity != null) {
            return existingIdentity.getLocalUserAccount();
        }

        String normalizedEmail = normalizeEmail(email);
        LocalUserAccount localAccount = localUserAccountRepository.findByUsernameIgnoreCase(normalizedEmail).orElse(null);
        if (localAccount != null) {
            throw new FederatedAccountLinkRequiredException(new FederatedAuthPendingLink(provider, subject, normalizedEmail));
        }

        LocalUserAccount created = localUserAccountRepository.save(
            new LocalUserAccount(normalizedEmail, passwordEncoder.encode(UUID.randomUUID().toString()), true)
        );
        createIdentityLink(created, provider, subject, normalizedEmail);
        return created;
    }

    /**
     * Confirms account linking by validating the local password and persisting the external identity link.
     */
    @Transactional
    public LocalUserAccount linkWithLocalPassword(FederatedAuthPendingLink pendingLink, String rawPassword) {
        if (pendingLink == null) {
            throw new IllegalArgumentException("Pending link data must be present");
        }

        LocalUserAccount localAccount = localUserAccountRepository.findByUsernameIgnoreCase(pendingLink.email())
            .orElseThrow(() -> new IllegalArgumentException("Local account not found for pending email"));

        if (!passwordEncoder.matches(rawPassword, localAccount.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid local password for account linking");
        }

        if (federatedUserIdentityRepository
            .findByProviderAndSubject(pendingLink.provider(), pendingLink.subject())
            .isEmpty()) {
            createIdentityLink(localAccount, pendingLink.provider(), pendingLink.subject(), pendingLink.email());
        }

        return localAccount;
    }

    private void createIdentityLink(LocalUserAccount localAccount, String provider, String subject, String email) {
        federatedUserIdentityRepository.save(
            new FederatedUserIdentity(localAccount, provider, subject, email, Instant.now())
        );
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("Federated account email is required");
        }
        return normalized;
    }
}
