package dev.ccosta.aisha.application.security;

public class LocalUserAccountUsernameAlreadyExistsException extends RuntimeException {

    public LocalUserAccountUsernameAlreadyExistsException(String username) {
        super("Local user username already exists: " + username);
    }
}
