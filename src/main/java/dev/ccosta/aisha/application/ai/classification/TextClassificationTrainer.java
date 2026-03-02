package dev.ccosta.aisha.application.ai.classification;

import java.util.List;
import java.util.Optional;

/**
 * Trains a text classification model from validated historical examples.
 *
 * @param <L> the label type associated with each example
 */
public interface TextClassificationTrainer<L> {

    /**
     * Trains a model from the provided examples.
     *
     * @param examples the validated examples that should become training signal
     * @return a trained model, or empty when the provided examples are insufficient
     */
    Optional<TextClassificationModel<L>> train(List<TextClassificationExample<L>> examples);
}
