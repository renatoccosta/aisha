package dev.ccosta.aisha.infrastructure.ai.naivebayes;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccosta.aisha.application.ai.classification.TextClassificationExample;
import dev.ccosta.aisha.application.ai.classification.TextClassificationModel;
import dev.ccosta.aisha.application.ai.classification.TextClassificationRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MultinomialNaiveBayesTextClassificationTrainerTest {

    @Test
    void shouldTrainModelAndPredictWithContextualTokens() {
        MultinomialNaiveBayesTextClassificationTrainer<Long> trainer = new MultinomialNaiveBayesTextClassificationTrainer<>();

        TextClassificationModel<Long> model = trainer.train(List.of(
            new TextClassificationExample<>( "mercado bairro", List.of("account-1", "kind-expense", "bucket-small"), 10L),
            new TextClassificationExample<>( "supermercado atacado", List.of("account-1", "kind-expense", "bucket-medium"), 10L),
            new TextClassificationExample<>( "uber viagem", List.of("account-1", "kind-expense", "bucket-small"), 20L),
            new TextClassificationExample<>( "taxi centro", List.of("account-1", "kind-expense", "bucket-small"), 20L)
        )).orElseThrow();

        assertThat(model.predict(new TextClassificationRequest(
            "mercado central",
            List.of("account-1", "kind-expense", "bucket-small")
        ))).isPresent()
            .get()
            .extracting(prediction -> prediction.label(), prediction -> prediction.modelName())
            .containsExactly(10L, "multinomial-naive-bayes-v2");
    }

    @Test
    void shouldFallbackToSingleLabelModelWhenHistoryHasSingleCategory() {
        MultinomialNaiveBayesTextClassificationTrainer<Long> trainer = new MultinomialNaiveBayesTextClassificationTrainer<>();

        TextClassificationModel<Long> model = trainer.train(List.of(
            new TextClassificationExample<>("salario empresa", List.of("account-1", "kind-income", "bucket-xlarge"), 99L),
            new TextClassificationExample<>("bonus anual", List.of("account-1", "kind-income", "bucket-large"), 99L)
        )).orElseThrow();

        assertThat(model.predict(new TextClassificationRequest(
            "salario",
            List.of("account-1", "kind-income", "bucket-xlarge")
        ))).isPresent()
            .get()
            .extracting(prediction -> prediction.label(), prediction -> prediction.confidence())
            .containsExactly(99L, 1.0d);
    }
}
