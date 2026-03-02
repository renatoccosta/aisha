package dev.ccosta.aisha.application.ai.classification;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Represents a trained text classification model ready to perform inference.
 *
 * @param <L> the label type predicted by the model
 */
public interface TextClassificationModel<L> extends Serializable {

    Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");
    
    Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");

    /**
     * Predicts the most likely label for the provided request.
     *
     * @param request the text and contextual tokens to classify
     * @return the predicted label with confidence and model metadata, or empty when inference is not possible
     */
    Optional<ClassificationPrediction<L>> predict(TextClassificationRequest request);

    /**
     * Returns the logical model implementation name.
     *
     * @return the model implementation name
     */
    String modelName();

    /**
     * Returns the feature pipeline version used to train the model.
     *
     * @return the feature pipeline version
     */
    String pipelineVersion();

    /**
     * Returns how many validated examples were used during training.
     *
     * @return the number of training examples
     */
    int trainingExampleCount();

    /**
     * Returns how many distinct labels the model can predict.
     *
     * @return the number of labels supported by the model
     */
    int labelCount();

    /**
     * Returns how many terms are present in the trained vocabulary.
     *
     * @return the vocabulary size
     */
    int vocabularySize();
    
    static List<String> tokenize(String document, List<String> contextTokens) {
        List<String> tokens = new ArrayList<>();
        if (document != null) {
            for (String piece : TOKEN_SPLIT_PATTERN.split(normalize(document))) {
                if (!piece.isBlank() && piece.length() > 1) {
                    tokens.add(piece);
                }
            }
        }

        if (contextTokens != null) {
            for (String contextToken : contextTokens) {
                String normalized = normalize(contextToken);
                if (!normalized.isBlank()) {
                    tokens.add(normalized);
                }
            }
        }

        return tokens;
    }

    static String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    static int[] toVector(List<String> tokens, Map<String, Integer> currentVocabularyIndex) {
        int[] vector = new int[currentVocabularyIndex.size()];
        for (String token : tokens) {
            Integer index = currentVocabularyIndex.get(token);
            if (index != null) {
                vector[index]++;
            }
        }
        return vector;
    }

    static int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

}
