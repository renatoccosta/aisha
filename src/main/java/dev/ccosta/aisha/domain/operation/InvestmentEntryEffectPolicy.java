package dev.ccosta.aisha.domain.operation;

import dev.ccosta.aisha.domain.entry.EntryEffect;

/**
 * Centralizes how investment domain events are reflected in equivalent financial entries.
 */
public final class InvestmentEntryEffectPolicy {

    private InvestmentEntryEffectPolicy() {
    }

    /**
     * Resolves the accounting effect that should be used by a financial entry linked to an investment operation.
     *
     * @param operationType investment operation type
     * @return RESULT for income/expense events and EQUITY for patrimonial movements
     */
    public static EntryEffect resolve(InvestmentOperationType operationType) {
        if (operationType == null) {
            return EntryEffect.EQUITY;
        }
        return switch (operationType) {
            case DIVIDEND, INTEREST, COUPON, FEE, TAX -> EntryEffect.RESULT;
            case BUY, SELL, AMORTIZATION, REDEMPTION, SPLIT, REVERSE_SPLIT, BONUS, SUBSCRIPTION, TRANSFER_IN, TRANSFER_OUT -> EntryEffect.EQUITY;
        };
    }

    /**
     * Resolves the accounting effect for the net financial entry created from an imported brokerage note.
     *
     * @return EQUITY because brokerage note settlements move money into or out of investment position accounting
     */
    public static EntryEffect resolveBrokerageNoteNetEntry() {
        return EntryEffect.EQUITY;
    }
}
