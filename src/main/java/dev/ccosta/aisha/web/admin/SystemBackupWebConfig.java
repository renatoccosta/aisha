package dev.ccosta.aisha.web.admin;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers web safeguards required by the system backup flow.
 */
@Configuration
public class SystemBackupWebConfig implements WebMvcConfigurer {

    private final SystemBackupWriteBlockInterceptor writeBlockInterceptor;

    public SystemBackupWebConfig(SystemBackupWriteBlockInterceptor writeBlockInterceptor) {
        this.writeBlockInterceptor = writeBlockInterceptor;
    }

    /**
     * Adds the backup write blocker to all MVC routes.
     *
     * @param registry interceptor registry supplied by Spring MVC
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(writeBlockInterceptor);
    }
}
