package dev.ccosta.aisha.infrastructure.ai.smile;

import static dev.ccosta.aisha.application.ai.classification.TextClassificationModel.sum;
import static dev.ccosta.aisha.application.ai.classification.TextClassificationModel.toVector;
import static dev.ccosta.aisha.application.ai.classification.TextClassificationModel.tokenize;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import dev.ccosta.aisha.application.ai.classification.ClassificationPrediction;
import dev.ccosta.aisha.application.ai.classification.TextClassificationModel;
import dev.ccosta.aisha.application.ai.classification.TextClassificationRequest;
import smile.classification.DiscreteNaiveBayes;

public class SmileDiscreteNaiveBayesTextClassificationModel<L> implements TextClassificationModel<L> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String modelName;
    private final String pipelineVersion;
    private final int trainingExampleCount;
    private final LinkedHashMap<L, Integer> labelIndex;
    private final LinkedHashMap<String, Integer> vocabularyIndex;
    private final DiscreteNaiveBayes classifier;

    public SmileDiscreteNaiveBayesTextClassificationModel(
        String modelName,
        String pipelineVersion,
        int trainingExampleCount,
        Map<L, Integer> labelIndex,
        Map<String, Integer> vocabularyIndex,
        DiscreteNaiveBayes classifier
    ) {
        this.modelName = modelName;
        this.pipelineVersion = pipelineVersion;
        this.trainingExampleCount = trainingExampleCount;
        this.labelIndex = new LinkedHashMap<>(labelIndex);
        this.vocabularyIndex = new LinkedHashMap<>(vocabularyIndex);
        this.classifier = classifier;
    }

    @Override
    public Optional<ClassificationPrediction<L>> predict(TextClassificationRequest request) {
        int[] query = toVector(tokenize(request.document(), request.contextTokens()), vocabularyIndex);
        if (sum(query) == 0) {
            return Optional.empty();
        }

        double[] posteriori = new double[labelIndex.size()];
        int predictedIndex = classifier.predict(query, posteriori);

        if (predictedIndex < 0 || predictedIndex >= posteriori.length) {
            return Optional.empty();
        }

        L label = labelIndex.entrySet()
            .stream()
            .filter(entry -> entry.getValue() == predictedIndex)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);

        if (label == null) {
            return Optional.empty();
        }

        return Optional.of(new ClassificationPrediction<>(label, posteriori[predictedIndex], modelName));
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
        return classifier.numClasses();
    }

    @Override
    public int vocabularySize() {
        return vocabularyIndex.size();
    }
    
}
