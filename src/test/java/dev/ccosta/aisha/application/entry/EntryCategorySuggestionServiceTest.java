package dev.ccosta.aisha.application.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.ai.classification.ClassificationPrediction;
import dev.ccosta.aisha.application.ai.classification.TextClassificationRequest;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.domain.category.Category;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntryCategorySuggestionServiceTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private EntryCategoryTextFeatures features;

    @Mock
    private EntryCategoryModelManager modelManager;

    @Test
    void shouldSuggestCategoryFromActiveModel() {
        EntryCategorySuggestionService service = new EntryCategorySuggestionService(categoryService, features, modelManager);
        when(features.toClassificationRequest(any())).thenReturn(
            new TextClassificationRequest("mercado central", java.util.List.of("account-1", "kind-expense", "bucket-small"))
        );
        when(modelManager.predict(any())).thenReturn(Optional.of(new ClassificationPrediction<>(10L, 0.82d, "multinomial-naive-bayes-v2")));

        Category supermarket = new Category();
        supermarket.setTitle("Supermercado");
        when(categoryService.findById(10L)).thenReturn(supermarket);

        Optional<EntryCategorySuggestion> suggestion = service.suggest(
            new EntryCategorySuggestionRequest(1L, "mercado central", new BigDecimal("-58.30"))
        );

        assertThat(suggestion).isPresent();
        assertThat(suggestion.get().category().getTitle()).isEqualTo("Supermercado");
        assertThat(suggestion.get().confidence()).isEqualTo(0.82d);
        verify(categoryService).findById(10L);
    }

    @Test
    void shouldNotSuggestCategoryWhenModelConfidenceIsBelowThreshold() {
        EntryCategorySuggestionService service = new EntryCategorySuggestionService(categoryService, features, modelManager);
        when(features.toClassificationRequest(any())).thenReturn(
            new TextClassificationRequest("mercado central", java.util.List.of("account-1", "kind-expense", "bucket-small"))
        );
        when(modelManager.predict(any())).thenReturn(Optional.of(new ClassificationPrediction<>(10L, 0.20d, "multinomial-naive-bayes-v2")));

        Optional<EntryCategorySuggestion> suggestion = service.suggest(
            new EntryCategorySuggestionRequest(1L, "mercado central", new BigDecimal("-58.30"))
        );

        assertThat(suggestion).isEmpty();
    }
}
