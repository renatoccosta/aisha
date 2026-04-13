package dev.ccosta.aisha.application.entry;

public record EntryTransferView(
    Long transferId,
    Long counterpartEntryId,
    Long counterpartAccountId,
    String counterpartAccountTitle,
    boolean originSide
) {
}
