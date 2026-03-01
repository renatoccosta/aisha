package dev.ccosta.aisha.application.ai.classification;

import java.util.List;
import java.util.Optional;

public interface TextClassifier<L> {

    Optional<ClassificationPrediction<L>> classify(
        TextClassificationRequest request,
        List<TextClassificationExample<L>> examples
    );
}
