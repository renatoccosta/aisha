package dev.ccosta.aisha.application.account;

public class AccountInUseException extends RuntimeException {

    public AccountInUseException(Long id) {
        super("Account in use: " + id);
    }
}
