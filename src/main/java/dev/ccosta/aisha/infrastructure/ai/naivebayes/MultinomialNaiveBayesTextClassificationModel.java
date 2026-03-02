package dev.ccosta.aisha.infrastructure.ai.naivebayes;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.ccosta.aisha.application.ai.classification.ClassificationPrediction;
import dev.ccosta.aisha.application.ai.classification.TextClassificationModel;
import static dev.ccosta.aisha.application.ai.classification.TextClassificationModel.*;
import dev.ccosta.aisha.application.ai.classification.TextClassificationRequest;

/**
 * Serializable multinomial Naive Bayes model for normalized text and contextual tokens.
 *
 * @param <L> the predicted label type
 */
public class MultinomialNaiveBayesTextClassificationModel<L> implements TextClassificationModel<L> {

    @Serial
    private static final long serialVersionUID = 1L;

    static final String DEFAULT_MODEL_NAME = "multinomial-naive-bayes-v2";
    static final String DEFAULT_PIPELINE_VERSION = "entry-category-features-v2";

    private final String modelName;
    private final String pipelineVersion;
    private final int trainingExampleCount;
    private final LinkedHashMap<L, Integer> labelIndex;
    private final LinkedHashMap<String, Integer> vocabularyIndex;
    private final long[] documentsPerLabel;
    private final long[] tokenTotalsPerLabel;
    private final long[][] tokenCountsByLabel;

    public MultinomialNaiveBayesTextClassificationModel(
        String modelName,
        String pipelineVersion,
        int trainingExampleCount,
        LinkedHashMap<L, Integer> labelIndex,
        LinkedHashMap<String, Integer> vocabularyIndex,
        long[] documentsPerLabel,
        long[] tokenTotalsPerLabel,
        long[][] tokenCountsByLabel
    ) {
        this.modelName = modelName;
        this.pipelineVersion = pipelineVersion;
        this.trainingExampleCount = trainingExampleCount;
        this.labelIndex = new LinkedHashMap<>(labelIndex);
        this.vocabularyIndex = new LinkedHashMap<>(vocabularyIndex);
        this.documentsPerLabel = documentsPerLabel.clone();
        this.tokenTotalsPerLabel = tokenTotalsPerLabel.clone();
        this.tokenCountsByLabel = cloneCounts(tokenCountsByLabel);
    }

    @Override
    public Optional<ClassificationPrediction<L>> predict(TextClassificationRequest request) {
        if (request == null) {
            return Optional.empty();
        }

        List<String> tokens = tokenize(request.document(), request.contextTokens());
        int[] query = toVector(tokens, vocabularyIndex);
        int tokenCount = sum(query);
        if (tokenCount == 0 || labelIndex.isEmpty() || vocabularyIndex.isEmpty()) {
            return Optional.empty();
        }

        double[] logProbabilities = new double[labelIndex.size()];
        double maxLogProbability = Double.NEGATIVE_INFINITY;
        int totalDocuments = trainingExampleCount;
        int vocabularySize = vocabularyIndex.size();

        for (Map.Entry<L, Integer> entry : labelIndex.entrySet()) {
            int labelPosition = entry.getValue();
            double logProbability = Math.log((documentsPerLabel[labelPosition] + 1.0d) / (totalDocuments + labelIndex.size()));
            double denominator = tokenTotalsPerLabel[labelPosition] + vocabularySize;

            for (int tokenIndex = 0; tokenIndex < query.length; tokenIndex++) {
                int count = query[tokenIndex];
                if (count <= 0) {
                    continue;
                }
                double tokenProbability = (tokenCountsByLabel[labelPosition][tokenIndex] + 1.0d) / denominator;
                logProbability += count * Math.log(tokenProbability);
            }

            logProbabilities[labelPosition] = logProbability;
            if (logProbability > maxLogProbability) {
                maxLogProbability = logProbability;
            }
        }

        double probabilitySum = 0.0d;
        double[] posteriori = new double[labelIndex.size()];
        for (int labelPosition = 0; labelPosition < logProbabilities.length; labelPosition++) {
            posteriori[labelPosition] = Math.exp(logProbabilities[labelPosition] - maxLogProbability);
            probabilitySum += posteriori[labelPosition];
        }

        if (probabilitySum <= 0.0d) {
            return Optional.empty();
        }

        int predictedIndex = -1;
        double highestPosterior = Double.NEGATIVE_INFINITY;
        for (int labelPosition = 0; labelPosition < posteriori.length; labelPosition++) {
            posteriori[labelPosition] = posteriori[labelPosition] / probabilitySum;
            if (posteriori[labelPosition] > highestPosterior) {
                highestPosterior = posteriori[labelPosition];
                predictedIndex = labelPosition;
            }
        }

        if (predictedIndex < 0) {
            return Optional.empty();
        }

        L predictedLabel = labelForIndex(predictedIndex);
        if (predictedLabel == null) {
            return Optional.empty();
        }

        return Optional.of(new ClassificationPrediction<>(predictedLabel, highestPosterior, modelName));
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
        return labelIndex.size();
    }

    @Override
    public int vocabularySize() {
        return vocabularyIndex.size();
    }

    private long[][] cloneCounts(long[][] source) {
        long[][] clone = new long[source.length][];
        for (int index = 0; index < source.length; index++) {
            clone[index] = source[index].clone();
        }
        return clone;
    }

    private L labelForIndex(int predictedIndex) {
        for (Map.Entry<L, Integer> entry : labelIndex.entrySet()) {
            if (entry.getValue() == predictedIndex) {
                return entry.getKey();
            }
        }
        return null;
    }
}
