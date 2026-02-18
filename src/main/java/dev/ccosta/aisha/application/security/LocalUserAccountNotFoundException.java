package dev.ccosta.aisha.application.security;

public class LocalUserAccountNotFoundException extends RuntimeException {

    public LocalUserAccountNotFoundException(Long id) {
        super("Local user account not found: " + id);
    }
}
