package dev.ccosta.aisha.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Unified authenticated principal used by both local and federated authentication flows.
 */
public class AishaPrincipal implements UserDetails, OidcUser, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long localUserId;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    public AishaPrincipal(
        Long localUserId,
        String username,
        String passwordHash,
        boolean enabled,
        Collection<? extends GrantedAuthority> authorities,
        Map<String, Object> attributes,
        OidcIdToken idToken,
        OidcUserInfo userInfo
    ) {
        this.localUserId = localUserId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = Collections.unmodifiableCollection(authorities);
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    public Long getLocalUserId() {
        return localUserId;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Map<String, Object> getClaims() {
        return attributes;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
