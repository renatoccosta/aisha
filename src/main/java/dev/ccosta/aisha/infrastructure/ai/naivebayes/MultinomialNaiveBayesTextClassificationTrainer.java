package dev.ccosta.aisha.infrastructure.ai.naivebayes;

import static dev.ccosta.aisha.application.ai.classification.TextClassificationModel.tokenize;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import dev.ccosta.aisha.application.ai.classification.TextClassificationExample;
import dev.ccosta.aisha.application.ai.classification.TextClassificationModel;
import dev.ccosta.aisha.application.ai.classification.TextClassificationTrainer;

/**
 * Trains a serializable multinomial Naive Bayes model from normalized bag-of-words examples.
 */
public class MultinomialNaiveBayesTextClassificationTrainer<L> implements TextClassificationTrainer<L> {

    @Override
    public Optional<TextClassificationModel<L>> train(List<TextClassificationExample<L>> examples) {
        if (examples == null || examples.isEmpty()) {
            return Optional.empty();
        }

        LinkedHashMap<L, Integer> labelIndex = new LinkedHashMap<>();
        LinkedHashSet<String> vocabularyTerms = new LinkedHashSet<>();
        List<List<String>> tokenizedExamples = new ArrayList<>();
        List<L> labels = new ArrayList<>();

        for (TextClassificationExample<L> example : examples) {
            if (example == null || example.label() == null) {
                continue;
            }

            List<String> tokens = tokenize(example.document(), example.contextTokens());
            if (tokens.isEmpty()) {
                continue;
            }

            labelIndex.computeIfAbsent(example.label(), ignored -> labelIndex.size());
            vocabularyTerms.addAll(tokens);
            tokenizedExamples.add(tokens);
            labels.add(example.label());
        }

        if (labelIndex.isEmpty()) {
            return Optional.empty();
        }

        if (labelIndex.size() == 1) {
            return Optional.of(new SingleLabelTextClassificationModel<>(
                labelIndex.keySet().iterator().next(),
                "historical-majority-v2",
                MultinomialNaiveBayesTextClassificationModel.DEFAULT_PIPELINE_VERSION,
                tokenizedExamples.size()
            ));
        }

        if (vocabularyTerms.isEmpty() || tokenizedExamples.isEmpty()) {
            return Optional.empty();
        }

        LinkedHashMap<String, Integer> vocabularyIndex = new LinkedHashMap<>();
        for (String token : vocabularyTerms) {
            vocabularyIndex.put(token, vocabularyIndex.size());
        }

        long[] documentsPerLabel = new long[labelIndex.size()];
        long[] tokenTotalsPerLabel = new long[labelIndex.size()];
        long[][] tokenCountsByLabel = new long[labelIndex.size()][vocabularyIndex.size()];

        for (int exampleIndex = 0; exampleIndex < tokenizedExamples.size(); exampleIndex++) {
            L label = labels.get(exampleIndex);
            int labelPosition = labelIndex.get(label);
            documentsPerLabel[labelPosition]++;
            for (String token : tokenizedExamples.get(exampleIndex)) {
                int tokenPosition = vocabularyIndex.get(token);
                tokenCountsByLabel[labelPosition][tokenPosition]++;
                tokenTotalsPerLabel[labelPosition]++;
            }
        }

        return Optional.of(new MultinomialNaiveBayesTextClassificationModel<>(
            MultinomialNaiveBayesTextClassificationModel.DEFAULT_MODEL_NAME,
            MultinomialNaiveBayesTextClassificationModel.DEFAULT_PIPELINE_VERSION,
            tokenizedExamples.size(),
            labelIndex,
            vocabularyIndex,
            documentsPerLabel,
            tokenTotalsPerLabel,
            tokenCountsByLabel
        ));
    }
}
