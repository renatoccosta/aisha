package dev.ccosta.aisha.application.entry;

import dev.ccosta.aisha.application.ai.classification.ClassificationPrediction;
import dev.ccosta.aisha.application.ai.classification.TextClassificationExample;
import dev.ccosta.aisha.application.ai.classification.TextClassificationRequest;
import dev.ccosta.aisha.application.ai.classification.TextClassifier;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.EntryCategoryTrainingExample;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntryCategorySuggestionService {

    private static final int MIN_TRAINING_EXAMPLES = 3;
    private static final double MIN_CONFIDENCE = 0.45d;

    private final EntryRepository entryRepository;
    private final CategoryService categoryService;
    private final TextClassifier<Long> textClassifier;

    public EntryCategorySuggestionService(
        EntryRepository entryRepository,
        CategoryService categoryService,
        TextClassifier<Long> textClassifier
    ) {
        this.entryRepository = entryRepository;
        this.categoryService = categoryService;
        this.textClassifier = textClassifier;
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

        List<EntryCategoryTrainingExample> trainingExamples = entryRepository.listCategoryTrainingExamples();
        if (trainingExamples.size() < MIN_TRAINING_EXAMPLES) {
            return Optional.empty();
        }

        Map<Long, Long> countsByCategory = trainingExamples.stream()
            .collect(Collectors.groupingBy(EntryCategoryTrainingExample::categoryId, Collectors.counting()));
        if (countsByCategory.isEmpty()) {
            return Optional.empty();
        }

        if (countsByCategory.size() == 1) {
            Long onlyCategoryId = countsByCategory.keySet().iterator().next();
            return Optional.of(new EntryCategorySuggestion(categoryService.findById(onlyCategoryId), 1.0d, "historical-majority"));
        }

        List<TextClassificationExample<Long>> examples = trainingExamples.stream()
            .map(example -> new TextClassificationExample<>(
                example.description(),
                buildContextTokens(example.accountId(), example.amount()),
                example.categoryId()
            ))
            .toList();

        Optional<ClassificationPrediction<Long>> prediction = textClassifier.classify(
            new TextClassificationRequest(description, buildContextTokens(request.accountId(), request.amount())),
            examples
        );
        if (prediction.isEmpty() || prediction.get().confidence() < MIN_CONFIDENCE) {
            return Optional.empty();
        }

        Category category = categoryService.findById(prediction.get().label());
        return Optional.of(new EntryCategorySuggestion(category, prediction.get().confidence(), prediction.get().modelName()));
    }

    private List<String> buildContextTokens(Long accountId, BigDecimal amount) {
        List<String> tokens = new ArrayList<>();
        tokens.add("account-" + accountId);
        tokens.add(amount.signum() < 0 ? "kind-expense" : "kind-income");
        tokens.add("bucket-" + amountBucket(amount.abs()));
        return tokens;
    }

    private String amountBucket(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("20.00")) < 0) {
            return "tiny";
        }
        if (amount.compareTo(new BigDecimal("100.00")) < 0) {
            return "small";
        }
        if (amount.compareTo(new BigDecimal("500.00")) < 0) {
            return "medium";
        }
        if (amount.compareTo(new BigDecimal("2000.00")) < 0) {
            return "large";
        }
        return "xlarge";
    }
}
