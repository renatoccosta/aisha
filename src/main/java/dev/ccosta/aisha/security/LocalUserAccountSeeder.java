package dev.ccosta.aisha.security;

import dev.ccosta.aisha.infrastructure.persistence.security.JpaLocalUserAccountRepository;
import dev.ccosta.aisha.infrastructure.persistence.security.LocalUserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class LocalUserAccountSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalUserAccountSeeder.class);

    private final JpaLocalUserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${aisha.security.seed.username:admin}")
    private String seedUsername;

    @Value("${aisha.security.seed.password:admin}")
    private String seedPassword;

    public LocalUserAccountSeeder(
        JpaLocalUserAccountRepository userAccountRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userAccountRepository.findByUsername(seedUsername).isPresent()) {
            return;
        }

        String hash = passwordEncoder.encode(seedPassword);
        userAccountRepository.save(new LocalUserAccount(seedUsername, hash, true));
        log.warn("Default local user created: {}. Change password in non-dev environments.", seedUsername);
    }
}
