package dev.ccosta.aisha.application.entry.transfer;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntryTransferCreationRequest(
    Long originAccountId,
    Long destinationAccountId,
    LocalDate movementDate,
    LocalDate settlementDate,
    String description,
    BigDecimal amount,
    String notes
) {
}
