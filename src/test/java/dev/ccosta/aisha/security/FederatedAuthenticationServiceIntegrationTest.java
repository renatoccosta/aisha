package dev.ccosta.aisha.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ccosta.aisha.infrastructure.persistence.security.JpaFederatedUserIdentityRepository;
import dev.ccosta.aisha.infrastructure.persistence.security.JpaLocalUserAccountRepository;
import dev.ccosta.aisha.infrastructure.persistence.security.LocalUserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FederatedAuthenticationServiceIntegrationTest {

    @Autowired
    private FederatedAuthenticationService federatedAuthenticationService;

    @Autowired
    private JpaLocalUserAccountRepository localUserAccountRepository;

    @Autowired
    private JpaFederatedUserIdentityRepository federatedUserIdentityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateNewLocalUserAndIdentityWhenEmailDoesNotExist() {
        LocalUserAccount account = federatedAuthenticationService.resolveOrCreateLocalAccount(
            "google", "subject-new", "novo.usuario@example.com"
        );

        assertThat(account.getId()).isNotNull();
        assertThat(account.getUsername()).isEqualTo("novo.usuario@example.com");
        assertThat(federatedUserIdentityRepository.findByProviderAndSubject("google", "subject-new")).isPresent();
    }

    @Test
    void shouldRequireLinkConfirmationWhenLocalEmailAlreadyExists() {
        localUserAccountRepository.save(new LocalUserAccount(
            "existente@example.com",
            passwordEncoder.encode("senha-secreta"),
            true
        ));

        assertThatThrownBy(() -> federatedAuthenticationService.resolveOrCreateLocalAccount(
            "google", "subject-existing", "existente@example.com"
        )).isInstanceOf(FederatedAccountLinkRequiredException.class);
    }

    @Test
    void shouldLinkExternalIdentityAfterValidLocalPasswordConfirmation() {
        localUserAccountRepository.save(new LocalUserAccount(
            "vinculo@example.com",
            passwordEncoder.encode("senha-local"),
            true
        ));

        LocalUserAccount account = federatedAuthenticationService.linkWithLocalPassword(
            new FederatedAuthPendingLink("google", "subject-link", "vinculo@example.com"),
            "senha-local"
        );

        assertThat(account.getUsername()).isEqualTo("vinculo@example.com");
        assertThat(federatedUserIdentityRepository.findByProviderAndSubject("google", "subject-link")).isPresent();
    }
}
