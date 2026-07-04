package dev.ccosta.aisha.application.brokeragenote.importing;

import dev.ccosta.aisha.domain.brokeragenote.BrokerageNote;
import dev.ccosta.aisha.domain.operation.InvestmentOperation;
import java.util.List;

/**
 * Carries a parsed brokerage note and its ready-to-import investment operations.
 *
 * @param brokerageNote parsed brokerage note data
 * @param operations operations extracted from the brokerage note
 */
public record ParsedBrokerageNote(
    BrokerageNote brokerageNote,
    List<InvestmentOperation> operations
) {

    public ParsedBrokerageNote {
        operations = operations == null ? List.of() : List.copyOf(operations);
    }
}
