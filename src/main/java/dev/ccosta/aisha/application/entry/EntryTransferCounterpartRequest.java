package dev.ccosta.aisha.application.entry;

import java.time.LocalDate;

public record EntryTransferCounterpartRequest(
    Long counterpartAccountId,
    LocalDate movementDate,
    LocalDate settlementDate,
    String description,
    String notes
) {
}
