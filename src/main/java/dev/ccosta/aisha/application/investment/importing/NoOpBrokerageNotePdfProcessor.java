package dev.ccosta.aisha.application.investment.importing;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Placeholder PDF processor used until brokerage note PDF parsing is implemented.
 */
@Component
public class NoOpBrokerageNotePdfProcessor implements BrokerageNotePdfProcessor {

    /**
     * Returns no parsed notes because PDF parsing is intentionally not implemented yet.
     *
     * @param request processing input
     * @return an empty parsed-note list
     */
    @Override
    public List<ParsedBrokerageNote> process(BrokerageNoteProcessingRequest request) {
        return List.of();
    }
}
