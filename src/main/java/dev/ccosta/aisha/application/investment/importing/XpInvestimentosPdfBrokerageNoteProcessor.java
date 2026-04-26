package dev.ccosta.aisha.application.investment.importing;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Placeholder processor for XP Investimentos PDF brokerage notes.
 */
@Component
public class XpInvestimentosPdfBrokerageNoteProcessor implements BrokerageNoteProcessor {

    /**
     * Checks whether the uploaded file looks like an XP Investimentos PDF brokerage note.
     *
     * @param request processing input
     * @return false until the XP PDF format detection is implemented
     */
    @Override
    public boolean supports(BrokerageNoteProcessingRequest request) {
        return false;
    }

    /**
     * Returns no parsed notes because XP PDF parsing is intentionally not implemented yet.
     *
     * @param request processing input
     * @return an empty parsed-note list
     */
    @Override
    public List<ParsedBrokerageNote> process(BrokerageNoteProcessingRequest request) {
        return List.of();
    }
}
