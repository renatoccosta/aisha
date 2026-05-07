package dev.ccosta.aisha.web.entry;

import dev.ccosta.aisha.application.entry.EntryRelationSummary;
import dev.ccosta.aisha.domain.entry.Entry;

/**
 * Describes which row or detail actions are available for a financial entry.
 */
public record EntryActionState(
    boolean detailsAllowed,
    boolean regularEditAllowed,
    boolean transferEditAllowed,
    boolean createLinkedOperationAllowed,
    boolean createCounterpartAllowed,
    boolean linkTransferAllowed,
    boolean unlinkTransferAllowed,
    boolean deleteAllowed
) {

    /**
     * Builds action flags from entry type and relationship summary.
     *
     * @param entry entry being rendered
     * @param relationSummary relationship summary for the entry
     * @return action state for the entry
     */
    static EntryActionState from(Entry entry, EntryRelationSummary relationSummary) {
        boolean transfer = entry.isTransfer() || relationSummary.hasTransfer();
        boolean lockedByInvestment = relationSummary.hasInvestmentOperation() || relationSummary.hasBrokerageNote();
        boolean commonWithoutRelationships = !transfer && !lockedByInvestment && !relationSummary.hasAnyRelationship();
        return new EntryActionState(
            true,
            commonWithoutRelationships,
            transfer && !lockedByInvestment,
            commonWithoutRelationships,
            commonWithoutRelationships,
            commonWithoutRelationships,
            transfer && !lockedByInvestment,
            commonWithoutRelationships
        );
    }
}
