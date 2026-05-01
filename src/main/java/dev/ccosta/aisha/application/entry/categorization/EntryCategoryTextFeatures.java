package dev.ccosta.aisha.application.entry.categorization;

import dev.ccosta.aisha.application.ai.classification.TextClassificationExample;
import dev.ccosta.aisha.application.ai.classification.TextClassificationRequest;
import dev.ccosta.aisha.domain.entry.categorization.EntryCategoryTrainingExample;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds the text and contextual features used by the entry category suggestion model.
 */
@Component
public class EntryCategoryTextFeatures {

    /**
     * Converts a validated historical entry into a training example.
     *
     * @param example the validated historical entry used as training signal
     * @return the corresponding text classification example
     */
    public TextClassificationExample<Long> toTrainingExample(EntryCategoryTrainingExample example) {
        return new TextClassificationExample<>(
            example.description(),
            buildContextTokens(example.accountId(), example.amount()),
            example.categoryId()
        );
    }

    /**
     * Converts a suggestion request into the inference request expected by the model.
     *
     * @param request the entry category suggestion request
     * @return the text classification request used during inference
     */
    public TextClassificationRequest toClassificationRequest(EntryCategorySuggestionRequest request) {
        return new TextClassificationRequest(
            request.description(),
            buildContextTokens(request.accountId(), request.amount())
        );
    }

    /**
     * Builds the contextual tokens appended to the free-text description.
     *
     * @param accountId the account owning the entry
     * @param amount the entry amount
     * @return the contextual feature tokens
     */
    public List<String> buildContextTokens(Long accountId, BigDecimal amount) {
        List<String> tokens = new ArrayList<>();
        tokens.add("account-" + accountId);
        tokens.add(amount.signum() < 0 ? "kind-expense" : "kind-income");
        tokens.add("bucket-" + amountBucket(amount.abs()));
        return tokens;
    }

    private String amountBucket(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("20.00")) < 0) {
            return "tiny";
        }
        if (amount.compareTo(new BigDecimal("100.00")) < 0) {
            return "small";
        }
        if (amount.compareTo(new BigDecimal("500.00")) < 0) {
            return "medium";
        }
        if (amount.compareTo(new BigDecimal("2000.00")) < 0) {
            return "large";
        }
        return "xlarge";
    }
}
