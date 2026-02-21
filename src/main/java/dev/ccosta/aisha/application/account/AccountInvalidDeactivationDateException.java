package dev.ccosta.aisha.application.account;

import java.time.LocalDate;

public class AccountInvalidDeactivationDateException extends RuntimeException {

    private final LocalDate deactivationDate;
    private final LocalDate latestSettlementDate;

    public AccountInvalidDeactivationDateException(LocalDate deactivationDate, LocalDate latestSettlementDate) {
        super("Account deactivation date must not be before latest settlement date");
        this.deactivationDate = deactivationDate;
        this.latestSettlementDate = latestSettlementDate;
    }

    public LocalDate getDeactivationDate() {
        return deactivationDate;
    }

    public LocalDate getLatestSettlementDate() {
        return latestSettlementDate;
    }
}
