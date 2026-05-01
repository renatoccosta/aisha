package dev.ccosta.aisha.application.entry.categorization;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.ccosta.aisha.application.ai.classification.ClassificationPrediction;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.application.entry.categorization.model.EntryCategoryModelManager;
import dev.ccosta.aisha.domain.category.Category;

@Service
public class EntryCategorySuggestionService {

    private static final double MIN_CONFIDENCE = 0.45d;

    private final CategoryService categoryService;
    private final EntryCategoryTextFeatures features;
    private final EntryCategoryModelManager modelManager;

    public EntryCategorySuggestionService(
        CategoryService categoryService,
        EntryCategoryTextFeatures features,
        EntryCategoryModelManager modelManager
    ) {
        this.categoryService = categoryService;
        this.features = features;
        this.modelManager = modelManager;
    }

    @Transactional(readOnly = true)
    public Optional<EntryCategorySuggestion> suggest(EntryCategorySuggestionRequest request) {
        if (request == null || request.accountId() == null || request.amount() == null || request.description() == null) {
            return Optional.empty();
        }

        String description = request.description().trim();
        if (description.isBlank()) {
            return Optional.empty();
        }

        Optional<ClassificationPrediction<Long>> prediction = modelManager.predict(
            features.toClassificationRequest(new EntryCategorySuggestionRequest(request.accountId(), description, request.amount()))
        );
        if (prediction.isEmpty() || prediction.get().confidence() < MIN_CONFIDENCE) {
            return Optional.empty();
        }

        Category category = categoryService.findById(prediction.get().label());
        return Optional.of(new EntryCategorySuggestion(category, prediction.get().confidence(), prediction.get().modelName()));
    }
}
