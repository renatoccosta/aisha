package dev.ccosta.aisha.security;

import dev.ccosta.aisha.infrastructure.persistence.security.JpaLocalUserAccountRepository;
import dev.ccosta.aisha.infrastructure.persistence.security.LocalUserAccount;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final JpaLocalUserAccountRepository userAccountRepository;

    public DatabaseUserDetailsService(JpaLocalUserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LocalUserAccount account = userAccountRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new AishaPrincipal(
            account.getId(),
            account.getUsername(),
            account.getPasswordHash(),
            account.isEnabled(),
            AuthorityUtils.createAuthorityList("ROLE_USER"),
            java.util.Map.of("provider", "local"),
            null,
            null
        );
    }
}
