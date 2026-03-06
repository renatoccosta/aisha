package dev.ccosta.aisha.security;

import dev.ccosta.aisha.infrastructure.logging.CorrelationIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AuditAuthenticationHandlers auditAuthenticationHandlers,
        FederatedAuthenticationFailureHandler federatedAuthenticationFailureHandler,
        AishaOidcUserService aishaOidcUserService,
        AbsoluteSessionTimeoutFilter absoluteSessionTimeoutFilter,
        CorrelationIdFilter correlationIdFilter,
        SessionRegistry sessionRegistry
    ) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/login",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/auth/federated/link",
                    "/css/**",
                    "/js/**",
                    "/img/**",
                    "/webjars/**",
                    "/error",
                    "/error/**",
                    "/debug/**",
                    "/favicon.ico",
                    "/actuator/health",
                    "/actuator/health/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.migrateSession())
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry)
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(auditAuthenticationHandlers)
                .failureHandler(auditAuthenticationHandlers)
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(aishaOidcUserService))
                .successHandler(auditAuthenticationHandlers)
                .failureHandler(federatedAuthenticationFailureHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutSuccessHandler(auditAuthenticationHandlers)
            )
            .csrf(Customizer.withDefaults())
            .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(absoluteSessionTimeoutFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
