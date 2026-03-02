package dev.ccosta.aisha.infrastructure.ai.naivebayes;

import dev.ccosta.aisha.application.ai.classification.ClassificationPrediction;
import dev.ccosta.aisha.application.ai.classification.TextClassificationModel;
import dev.ccosta.aisha.application.ai.classification.TextClassificationRequest;
import java.util.List;
import java.util.Optional;

/**
 * Predicts a single validated label when historical data contains only one possible outcome.
 *
 * @param <L> the predicted label type
 */
public class SingleLabelTextClassificationModel<L> implements TextClassificationModel<L> {

    private final L label;
    private final String modelName;
    private final String pipelineVersion;
    private final int trainingExampleCount;

    public SingleLabelTextClassificationModel(L label, String modelName, String pipelineVersion, int trainingExampleCount) {
        this.label = label;
        this.modelName = modelName;
        this.pipelineVersion = pipelineVersion;
        this.trainingExampleCount = trainingExampleCount;
    }

    @Override
    public Optional<ClassificationPrediction<L>> predict(TextClassificationRequest request) {
        if (request == null || isBlank(request.document()) || noContext(request.contextTokens())) {
            return Optional.empty();
        }
        return Optional.of(new ClassificationPrediction<>(label, 1.0d, modelName));
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public String pipelineVersion() {
        return pipelineVersion;
    }

    @Override
    public int trainingExampleCount() {
        return trainingExampleCount;
    }

    @Override
    public int labelCount() {
        return 1;
    }

    @Override
    public int vocabularySize() {
        return 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private boolean noContext(List<String> contextTokens) {
        return contextTokens == null || contextTokens.stream().allMatch(token -> token == null || token.isBlank());
    }
}
