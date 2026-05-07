package dev.ccosta.aisha.application.entry;

import dev.ccosta.aisha.application.entry.transfer.EntryTransferView;
import java.util.Optional;

/**
 * Summarizes the relationships that affect available actions for one financial entry.
 *
 * @param entryId entry identifier
 * @param transferView transfer relationship details, when the entry belongs to a transfer
 * @param investmentOperationId linked investment operation identifier, when present
 * @param brokerageNoteId linked brokerage note identifier, when present
 */
public record EntryRelationSummary(
    Long entryId,
    EntryTransferView transferView,
    Long investmentOperationId,
    Long brokerageNoteId
) {

    /**
     * Creates an empty relationship summary for an entry.
     *
     * @param entryId entry identifier
     * @return a summary with no relationships
     */
    public static EntryRelationSummary empty(Long entryId) {
        return new EntryRelationSummary(entryId, null, null, null);
    }

    /**
     * Returns whether the entry belongs to a transfer.
     *
     * @return true when a transfer relationship exists
     */
    public boolean hasTransfer() {
        return transferView != null;
    }

    /**
     * Returns whether the entry is linked to an investment operation.
     *
     * @return true when an investment operation relationship exists
     */
    public boolean hasInvestmentOperation() {
        return investmentOperationId != null;
    }

    /**
     * Returns whether the entry is linked to a brokerage note.
     *
     * @return true when a brokerage note relationship exists
     */
    public boolean hasBrokerageNote() {
        return brokerageNoteId != null;
    }

    /**
     * Returns whether the entry has any relationship that restricts regular-entry actions.
     *
     * @return true when at least one supported relationship exists
     */
    public boolean hasAnyRelationship() {
        return hasTransfer() || hasInvestmentOperation() || hasBrokerageNote();
    }

    /**
     * Returns a copy with transfer information.
     *
     * @param transferView transfer details
     * @return updated summary
     */
    public EntryRelationSummary withTransferView(EntryTransferView transferView) {
        return new EntryRelationSummary(entryId, transferView, investmentOperationId, brokerageNoteId);
    }

    /**
     * Returns a copy with investment operation information.
     *
     * @param investmentOperationId linked operation identifier
     * @return updated summary
     */
    public EntryRelationSummary withInvestmentOperationId(Long investmentOperationId) {
        return new EntryRelationSummary(entryId, transferView, investmentOperationId, brokerageNoteId);
    }

    /**
     * Returns a copy with brokerage note information.
     *
     * @param brokerageNoteId linked brokerage note identifier
     * @return updated summary
     */
    public EntryRelationSummary withBrokerageNoteId(Long brokerageNoteId) {
        return new EntryRelationSummary(entryId, transferView, investmentOperationId, brokerageNoteId);
    }

    /**
     * Returns transfer details as an optional value for application flows that prefer null-free access.
     *
     * @return optional transfer view
     */
    public Optional<EntryTransferView> transferViewOptional() {
        return Optional.ofNullable(transferView);
    }
}
