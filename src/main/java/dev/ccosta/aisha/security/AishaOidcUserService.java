package dev.ccosta.aisha.security;

import dev.ccosta.aisha.infrastructure.persistence.security.LocalUserAccount;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Converts a successful OIDC login into AI$HA's unified internal principal.
 */
@Service
public class AishaOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final String ACCOUNT_LINK_REQUIRED = "account_link_required";

    private final OidcUserService delegate = new OidcUserService();
    private final FederatedAuthenticationService federatedAuthenticationService;

    public AishaOidcUserService(FederatedAuthenticationService federatedAuthenticationService) {
        this.federatedAuthenticationService = federatedAuthenticationService;
    }

    /**
     * Loads user info from provider and resolves local account linkage/creation.
     */
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String subject = oidcUser.getSubject();
        String email = oidcUser.getEmail();

        LocalUserAccount localAccount;
        try {
            localAccount = federatedAuthenticationService.resolveOrCreateLocalAccount(provider, subject, email);
        } catch (FederatedAccountLinkRequiredException ex) {
            FederatedAuthPendingLink pendingLink = ex.getPendingLink();
            String description = String.join("|", pendingLink.provider(), pendingLink.subject(), pendingLink.email());
            throw new OAuth2AuthenticationException(new OAuth2Error(ACCOUNT_LINK_REQUIRED, description, null));
        }

        Map<String, Object> attributes = new HashMap<>(oidcUser.getClaims());
        attributes.put("provider", provider);

        return new AishaPrincipal(
            localAccount.getId(),
            localAccount.getUsername(),
            localAccount.getPasswordHash(),
            localAccount.isEnabled(),
            AuthorityUtils.createAuthorityList("ROLE_USER"),
            attributes,
            oidcUser.getIdToken(),
            new OidcUserInfo(attributes)
        );
    }
}
