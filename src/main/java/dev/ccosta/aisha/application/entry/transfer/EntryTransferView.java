package dev.ccosta.aisha.application.entry.transfer;

public record EntryTransferView(
    Long transferId,
    Long counterpartEntryId,
    Long counterpartAccountId,
    String counterpartAccountTitle,
    boolean originSide
) {
}
