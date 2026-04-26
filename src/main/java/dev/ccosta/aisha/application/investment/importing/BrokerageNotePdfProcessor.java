package dev.ccosta.aisha.application.investment.importing;

import java.util.List;

/**
 * Converts brokerage note PDF content into domain objects ready for import.
 */
public interface BrokerageNotePdfProcessor {

    /**
     * Processes uploaded PDF bytes into parsed brokerage notes.
     *
     * @param request processing input
     * @return parsed brokerage notes and operations
     */
    List<ParsedBrokerageNote> process(BrokerageNoteProcessingRequest request);
}
