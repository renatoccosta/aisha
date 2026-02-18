package dev.ccosta.aisha.application.security;

public class LocalUserAccountSelfDeletionException extends RuntimeException {

    public LocalUserAccountSelfDeletionException(String username) {
        super("Authenticated local user cannot be deleted: " + username);
    }
}
