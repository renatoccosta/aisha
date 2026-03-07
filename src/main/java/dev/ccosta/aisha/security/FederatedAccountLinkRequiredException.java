package dev.ccosta.aisha.security;

/**
 * Signals that federated authentication found an existing local account and requires password confirmation
 * before linking the external identity.
 */
public class FederatedAccountLinkRequiredException extends RuntimeException {

    private final FederatedAuthPendingLink pendingLink;

    public FederatedAccountLinkRequiredException(FederatedAuthPendingLink pendingLink) {
        super("Federated account link confirmation is required");
        this.pendingLink = pendingLink;
    }

    public FederatedAuthPendingLink getPendingLink() {
        return pendingLink;
    }
}
