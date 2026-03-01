package dev.ccosta.aisha.application.ai.classification;

public record ClassificationPrediction<L>(
    L label,
    double confidence,
    String modelName
) {
}
