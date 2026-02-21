package dev.ccosta.aisha.application.security;

import dev.ccosta.aisha.infrastructure.persistence.security.JpaLocalUserAccountRepository;
import dev.ccosta.aisha.infrastructure.persistence.security.LocalUserAccount;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LocalUserAccountService {

    private final JpaLocalUserAccountRepository localUserAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalUserAccountService(
        JpaLocalUserAccountRepository localUserAccountRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.localUserAccountRepository = localUserAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<LocalUserAccount> listAllOrdered() {
        return localUserAccountRepository.findAllByOrderByUsernameAsc();
    }

    @Transactional(readOnly = true)
    public PagedResult<LocalUserAccount> listPageOrdered(int page, int pageSize) {
        Page<LocalUserAccount> result = localUserAccountRepository.findAllByOrderByUsernameAsc(PageRequest.of(page, pageSize));
        return new PagedResult<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public LocalUserAccount findById(Long id) {
        return localUserAccountRepository.findById(id)
            .orElseThrow(() -> new LocalUserAccountNotFoundException(id));
    }

    @Transactional
    public LocalUserAccount create(String username, String rawPassword, boolean enabled) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedPassword = normalizePassword(rawPassword);
        ensureUsernameIsUnique(normalizedUsername, null);

        LocalUserAccount account = new LocalUserAccount(
            normalizedUsername,
            passwordEncoder.encode(normalizedPassword),
            enabled
        );

        return localUserAccountRepository.save(account);
    }

    @Transactional
    public LocalUserAccount update(Long id, String username, String rawPassword, boolean enabled) {
        LocalUserAccount account = findById(id);
        String normalizedUsername = normalizeUsername(username);

        ensureUsernameIsUnique(normalizedUsername, id);
        account.setUsername(normalizedUsername);
        account.setEnabled(enabled);

        if (StringUtils.hasText(rawPassword)) {
            account.setPasswordHash(passwordEncoder.encode(normalizePassword(rawPassword)));
        }

        return localUserAccountRepository.save(account);
    }

    @Transactional
    public void deleteById(Long id, String authenticatedUsername) {
        LocalUserAccount account = findById(id);
        ensureNotDeletingAuthenticatedUser(account, authenticatedUsername);
        localUserAccountRepository.deleteById(id);
    }

    @Transactional
    public void bulkDelete(Collection<Long> ids, String authenticatedUsername) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(ids);
        for (Long id : uniqueIds) {
            LocalUserAccount account = findById(id);
            ensureNotDeletingAuthenticatedUser(account, authenticatedUsername);
        }

        localUserAccountRepository.deleteByIds(uniqueIds);
    }

    private void ensureUsernameIsUnique(String username, Long currentId) {
        localUserAccountRepository.findByUsernameIgnoreCase(username)
            .ifPresent(existing -> {
                if (currentId == null || !Objects.equals(existing.getId(), currentId)) {
                    throw new LocalUserAccountUsernameAlreadyExistsException(username);
                }
            });
    }

    private void ensureNotDeletingAuthenticatedUser(LocalUserAccount account, String authenticatedUsername) {
        if (authenticatedUsername != null && account.getUsername().equalsIgnoreCase(authenticatedUsername)) {
            throw new LocalUserAccountSelfDeletionException(authenticatedUsername);
        }
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("Local user username must not be blank");
        }
        return normalized;
    }

    private String normalizePassword(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("Local user password must not be blank");
        }
        return rawPassword;
    }
}
