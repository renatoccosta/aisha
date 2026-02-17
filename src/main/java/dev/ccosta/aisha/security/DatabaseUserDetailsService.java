package dev.ccosta.aisha.security;

import dev.ccosta.aisha.infrastructure.persistence.security.JpaLocalUserAccountRepository;
import dev.ccosta.aisha.infrastructure.persistence.security.LocalUserAccount;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
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
        LocalUserAccount account = userAccountRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new User(
            account.getUsername(),
            account.getPasswordHash(),
            account.isEnabled(),
            true,
            true,
            true,
            AuthorityUtils.createAuthorityList("ROLE_USER")
        );
    }
}
