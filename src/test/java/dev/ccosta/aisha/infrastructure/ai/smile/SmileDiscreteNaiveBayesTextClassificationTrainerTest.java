package dev.ccosta.aisha.infrastructure.ai.smile;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccosta.aisha.application.ai.classification.TextClassificationExample;
import dev.ccosta.aisha.application.ai.classification.TextClassificationModel;
import dev.ccosta.aisha.application.ai.classification.TextClassificationRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SmileDiscreteNaiveBayesTextClassificationTrainerTest {

    @Test
    void shouldReturnEmptyWhenExamplesAreNullOrEmpty() {
        SmileDiscreteNaiveBayesTextClassificationTrainer<Long> trainer = new SmileDiscreteNaiveBayesTextClassificationTrainer<>();

        assertThat(trainer.train(null)).isEmpty();
        assertThat(trainer.train(List.of())).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenExamplesDoNotProduceValidTrainingData() {
        SmileDiscreteNaiveBayesTextClassificationTrainer<Long> trainer = new SmileDiscreteNaiveBayesTextClassificationTrainer<>();

        List<TextClassificationExample<Long>> examples = new ArrayList<>();
        examples.add(null);
        examples.add(new TextClassificationExample<>(null, List.of(" "), null));
        examples.add(new TextClassificationExample<>("a", List.of(), 10L));
        examples.add(new TextClassificationExample<>(" ", List.of(""), 20L));

        assertThat(trainer.train(examples)).isEmpty();
    }

    @Test
    void shouldTrainSmileModelAndPredictUsingNormalizedTokens() {
        SmileDiscreteNaiveBayesTextClassificationTrainer<Long> trainer = new SmileDiscreteNaiveBayesTextClassificationTrainer<>();

        TextClassificationModel<Long> model = trainer.train(List.of(
            new TextClassificationExample<>("Mercado do bairro", List.of("account-1", "kind-expense", "bucket-small"), 10L),
            new TextClassificationExample<>("Supermercado atacado", List.of("account-1", "kind-expense", "bucket-medium"), 10L),
            new TextClassificationExample<>("Uber viagem centro", List.of("account-1", "kind-expense", "bucket-small"), 20L),
            new TextClassificationExample<>("Taxi aeroporto", List.of("account-1", "kind-expense", "bucket-small"), 20L)
        )).orElseThrow();

        assertThat(model.modelName()).isEqualTo("smile-discrete-naive-bayes-v1");
        assertThat(model.pipelineVersion()).isEqualTo("entry-category-features-v1");
        assertThat(model.trainingExampleCount()).isEqualTo(4);
        assertThat(model.labelCount()).isEqualTo(2);
        assertThat(model.vocabularySize()).isGreaterThan(0);

        assertThat(model.predict(new TextClassificationRequest(
            "mercádo central",
            List.of("account-1", "kind-expense", "bucket-small")
        ))).isPresent()
            .get()
            .extracting(prediction -> prediction.label(), prediction -> prediction.modelName())
            .containsExactly(10L, "smile-discrete-naive-bayes-v1");
    }

    @Test
    void shouldSerializeAndDeserializeTrainedSmileModel() throws Exception {
        SmileDiscreteNaiveBayesTextClassificationTrainer<Long> trainer = new SmileDiscreteNaiveBayesTextClassificationTrainer<>();

        TextClassificationModel<Long> model = trainer.train(List.of(
            new TextClassificationExample<>("Salario empresa", List.of("account-1", "kind-income", "bucket-xlarge"), 99L),
            new TextClassificationExample<>("Bonus anual", List.of("account-1", "kind-income", "bucket-large"), 99L),
            new TextClassificationExample<>("Restaurante centro", List.of("account-1", "kind-expense", "bucket-medium"), 55L),
            new TextClassificationExample<>("Almoco executivo", List.of("account-1", "kind-expense", "bucket-small"), 55L)
        )).orElseThrow();

        ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutput)) {
            objectOutputStream.writeObject(model);
        }

        TextClassificationModel<Long> deserializedModel;
        try (ObjectInputStream objectInputStream = new ObjectInputStream(
            new ByteArrayInputStream(byteArrayOutput.toByteArray())
        )) {
            @SuppressWarnings("unchecked")
            TextClassificationModel<Long> loadedModel = (TextClassificationModel<Long>) objectInputStream.readObject();
            deserializedModel = loadedModel;
        }

        assertThat(deserializedModel.predict(new TextClassificationRequest(
            "salario mensal",
            List.of("account-1", "kind-income", "bucket-xlarge")
        ))).isPresent()
            .get()
            .extracting(prediction -> prediction.label(), prediction -> prediction.modelName())
            .containsExactly(99L, "smile-discrete-naive-bayes-v1");
    }
}
