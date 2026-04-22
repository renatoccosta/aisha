package dev.ccosta.aisha.application.entry.importing.statement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntryStatementImportRecord(
    int rowPosition,
    LocalDate movementDate,
    LocalDate settlementDate,
    String description,
    BigDecimal amount,
    String notes,
    String externalId
) {
}
