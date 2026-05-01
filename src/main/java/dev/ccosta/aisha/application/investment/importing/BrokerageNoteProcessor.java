package dev.ccosta.aisha.application.investment.importing;

import java.util.List;

/**
 * Converts brokerage note file content into domain objects ready for import.
 */
public interface BrokerageNoteProcessor {

    /**
     * Checks whether this processor can parse the uploaded brokerage note file.
     *
     * @param request processing input
     * @return true when this processor supports the file format
     */
    boolean supports(BrokerageNoteProcessingRequest request);

    /**
     * Processes uploaded file bytes into parsed brokerage notes.
     *
     * @param request processing input
     * @return parsed brokerage notes and operations
     */
    List<ParsedBrokerageNote> process(BrokerageNoteProcessingRequest request);
}
