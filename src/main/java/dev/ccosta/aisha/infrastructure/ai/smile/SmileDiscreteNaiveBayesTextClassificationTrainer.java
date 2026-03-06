package dev.ccosta.aisha.infrastructure.ai.smile;

import static dev.ccosta.aisha.application.ai.classification.TextClassificationModel.toVector;
import static dev.ccosta.aisha.application.ai.classification.TextClassificationModel.tokenize;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import dev.ccosta.aisha.application.ai.classification.TextClassificationExample;
import dev.ccosta.aisha.application.ai.classification.TextClassificationModel;
import dev.ccosta.aisha.application.ai.classification.TextClassificationTrainer;
import smile.classification.DiscreteNaiveBayes;

@Component
public class SmileDiscreteNaiveBayesTextClassificationTrainer<L> implements TextClassificationTrainer<L> {

    private static final String MODEL_NAME = "smile-discrete-naive-bayes-v1";
    
    private static final String PIPELINE_VERSION = "entry-category-features-v1";

    @Override
    public Optional<TextClassificationModel<L>> train(List<TextClassificationExample<L>> examples) {
        if (examples == null || examples.isEmpty()) {
            return Optional.empty();
        }

        Map<L, Integer> labelIndex = new LinkedHashMap<>();
        List<List<String>> tokenizedExamples = new ArrayList<>(examples.size());
        LinkedHashSet<String> vocabularyTerms = new LinkedHashSet<>();

        for (TextClassificationExample<L> example : examples) {
            if (example == null || example.label() == null) {
                continue;
            }

            List<String> tokens = tokenize(
                    example.document(), example.contextTokens());
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

            List<String> tokens = tokenize(
                example.document(), example.contextTokens());
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

        return Optional.of(new SmileDiscreteNaiveBayesTextClassificationModel<>(
            MODEL_NAME,
            PIPELINE_VERSION,
            examples.size(),
            labelIndex,
            vocabularyIndex,
            classifier
        ));
    }

}
