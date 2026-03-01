package dev.ccosta.aisha.application.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.EntryCategoryTrainingExample;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.infrastructure.ai.smile.SmileNaiveBayesTextClassifier;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntryCategorySuggestionServiceTest {

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private CategoryService categoryService;

    @Test
    void shouldSuggestCategoryFromValidatedHistory() {
        EntryCategorySuggestionService service = new EntryCategorySuggestionService(
            entryRepository,
            categoryService,
            new SmileNaiveBayesTextClassifier<>()
        );
        when(entryRepository.listCategoryTrainingExamples()).thenReturn(List.of(
            new EntryCategoryTrainingExample(1L, "mercado bairro", new BigDecimal("-45.90"), 10L),
            new EntryCategoryTrainingExample(1L, "supermercado atacado", new BigDecimal("-120.00"), 10L),
            new EntryCategoryTrainingExample(1L, "compra mercado", new BigDecimal("-32.50"), 10L),
            new EntryCategoryTrainingExample(1L, "uber viagem", new BigDecimal("-18.00"), 20L),
            new EntryCategoryTrainingExample(1L, "taxi centro", new BigDecimal("-25.00"), 20L),
            new EntryCategoryTrainingExample(1L, "corrida aplicativo", new BigDecimal("-16.90"), 20L)
        ));

        Category supermarket = new Category();
        supermarket.setTitle("Supermercado");
        when(categoryService.findById(10L)).thenReturn(supermarket);

        Optional<EntryCategorySuggestion> suggestion = service.suggest(
            new EntryCategorySuggestionRequest(1L, "mercado central", new BigDecimal("-58.30"))
        );

        assertThat(suggestion).isPresent();
        assertThat(suggestion.get().category().getTitle()).isEqualTo("Supermercado");
        assertThat(suggestion.get().confidence()).isGreaterThan(0.45d);
        verify(categoryService).findById(10L);
    }

    @Test
    void shouldNotSuggestCategoryWithoutEnoughTrainingData() {
        EntryCategorySuggestionService service = new EntryCategorySuggestionService(
            entryRepository,
            categoryService,
            new SmileNaiveBayesTextClassifier<>()
        );
        when(entryRepository.listCategoryTrainingExamples()).thenReturn(List.of(
            new EntryCategoryTrainingExample(1L, "mercado bairro", new BigDecimal("-45.90"), 10L),
            new EntryCategoryTrainingExample(1L, "supermercado atacado", new BigDecimal("-120.00"), 10L)
        ));

        Optional<EntryCategorySuggestion> suggestion = service.suggest(
            new EntryCategorySuggestionRequest(1L, "mercado central", new BigDecimal("-58.30"))
        );

        assertThat(suggestion).isEmpty();
    }
}
