package dev.ccosta.aisha.infrastructure.ai.smile;

import dev.ccosta.aisha.application.ai.classification.ClassificationPrediction;
import dev.ccosta.aisha.application.ai.classification.TextClassificationExample;
import dev.ccosta.aisha.application.ai.classification.TextClassificationRequest;
import dev.ccosta.aisha.application.ai.classification.TextClassifier;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import smile.classification.DiscreteNaiveBayes;

@Component
public class SmileNaiveBayesTextClassifier<L> implements TextClassifier<L> {

    private static final String MODEL_NAME = "smile-discrete-naive-bayes-v1";
    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");

    @Override
    public Optional<ClassificationPrediction<L>> classify(
        TextClassificationRequest request,
        List<TextClassificationExample<L>> examples
    ) {
        if (request == null || examples == null || examples.isEmpty()) {
            return Optional.empty();
        }

        Map<L, Integer> labelIndex = new LinkedHashMap<>();
        List<List<String>> tokenizedExamples = new ArrayList<>(examples.size());
        LinkedHashSet<String> vocabularyTerms = new LinkedHashSet<>();

        for (TextClassificationExample<L> example : examples) {
            if (example == null || example.label() == null) {
                continue;
            }

            List<String> tokens = tokenize(example.document(), example.contextTokens());
            if (tokens.isEmpty()) {
                continue;
            }

            labelIndex.computeIfAbsent(example.label(), ignored -> labelIndex.size());
            tokenizedExamples.add(tokens);
            vocabularyTerms.addAll(tokens);
        }

        if (labelIndex.isEmpty() || vocabularyTerms.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Integer> vocabularyIndex = new LinkedHashMap<>();
        for (String token : vocabularyTerms) {
            vocabularyIndex.put(token, vocabularyIndex.size());
        }

        int[][] x = new int[tokenizedExamples.size()][vocabularyIndex.size()];
        int[] y = new int[tokenizedExamples.size()];

        int rowIndex = 0;
        for (TextClassificationExample<L> example : examples) {
            if (example == null || example.label() == null) {
                continue;
            }

            List<String> tokens = tokenize(example.document(), example.contextTokens());
            if (tokens.isEmpty()) {
                continue;
            }

            x[rowIndex] = toVector(tokens, vocabularyIndex);
            y[rowIndex] = labelIndex.get(example.label());
            rowIndex++;
        }

        if (rowIndex == 0) {
            return Optional.empty();
        }

        DiscreteNaiveBayes classifier = new DiscreteNaiveBayes(
            DiscreteNaiveBayes.Model.MULTINOMIAL,
            labelIndex.size(),
            vocabularyIndex.size()
        );
        classifier.update(x, y);

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

        return Optional.of(new ClassificationPrediction<>(label, posteriori[predictedIndex], MODEL_NAME));
    }

    private List<String> tokenize(String document, List<String> contextTokens) {
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

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private int[] toVector(List<String> tokens, Map<String, Integer> vocabularyIndex) {
        int[] vector = new int[vocabularyIndex.size()];
        for (String token : tokens) {
            Integer index = vocabularyIndex.get(token);
            if (index != null) {
                vector[index]++;
            }
        }
        return vector;
    }

    private int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }
}
