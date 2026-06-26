package dev.ccosta.aisha.application.backup;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides backup-related infrastructure beans.
 */
@Configuration
public class BackupApplicationConfig {

    /**
     * Provides the application clock used by backup timestamping.
     *
     * @return the system default-zone clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
