package dev.ccosta.aisha.security;

import java.io.Serial;
import java.io.Serializable;

/**
 * Session-scoped state used to confirm linking an external identity to an existing local account.
 */
public record FederatedAuthPendingLink(String provider, String subject, String email) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
