package dev.ccosta.aisha.application.entry;

import java.time.LocalDate;

public class EntrySettlementAfterAccountDeactivationException extends RuntimeException {

    private final LocalDate settlementDate;
    private final LocalDate accountDeactivationDate;

    public EntrySettlementAfterAccountDeactivationException(LocalDate settlementDate, LocalDate accountDeactivationDate) {
        super("Settlement date is after account deactivation date");
        this.settlementDate = settlementDate;
        this.accountDeactivationDate = accountDeactivationDate;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public LocalDate getAccountDeactivationDate() {
        return accountDeactivationDate;
    }
}
