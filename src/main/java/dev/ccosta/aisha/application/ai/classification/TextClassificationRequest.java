package dev.ccosta.aisha.application.ai.classification;

import java.util.List;

public record TextClassificationRequest(
    String document,
    List<String> contextTokens
) {
}
