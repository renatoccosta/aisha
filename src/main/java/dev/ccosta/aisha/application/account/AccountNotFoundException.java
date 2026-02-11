package dev.ccosta.aisha.application.account;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long id) {
        super("Account not found: " + id);
    }
}
