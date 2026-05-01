package dev.ccosta.aisha.application.entry.categorization.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.ccosta.aisha.application.ai.classification.TextClassificationExample;
import dev.ccosta.aisha.application.ai.classification.TextClassificationModel;
import dev.ccosta.aisha.application.ai.classification.TextClassificationTrainer;
import dev.ccosta.aisha.application.entry.categorization.EntryCategoryTextFeatures;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.entry.categorization.model.EntryCategorySuggestionModelArtifact;
import dev.ccosta.aisha.domain.entry.categorization.model.EntryCategorySuggestionModelRepository;
import dev.ccosta.aisha.domain.entry.categorization.model.EntryCategorySuggestionModelStatus;

/**
 * Builds, serializes, and persists entry category suggestion model versions.
 */
@Service
public class EntryCategoryModelTrainingService {

    private static final int MIN_TRAINING_EXAMPLES = 3;

    private final EntryRepository entryRepository;
    private final EntryCategoryTextFeatures features;
    private final TextClassificationTrainer<Long> trainer;
    private final EntryCategorySuggestionModelRepository modelRepository;

    public EntryCategoryModelTrainingService(
        EntryRepository entryRepository,
        EntryCategoryTextFeatures features,
        TextClassificationTrainer<Long> trainer,
        EntryCategorySuggestionModelRepository modelRepository
    ) {
        this.entryRepository = entryRepository;
        this.features = features;
        this.trainer = trainer;
        this.modelRepository = modelRepository;
    }

    /**
     * Trains and persists a new model version for the given trigger.
     *
     * @param trigger the event that requested this training cycle
     * @return the persisted artifact together with the reconstructed in-memory model
     */
    @Transactional(noRollbackFor = IllegalStateException.class)
    public EntryCategoryModelSnapshot trainAndPersist(EntryCategoryModelTrainingTrigger trigger) {
        EntryCategorySuggestionModelArtifact artifact = startArtifact(trigger);
        try {
            List<TextClassificationExample<Long>> examples = entryRepository.listCategoryTrainingExamples()
                .stream()
                .map(features::toTrainingExample)
                .toList();
            if (examples.size() < MIN_TRAINING_EXAMPLES) {
                throw new IllegalStateException("At least 3 validated examples are required to train the model");
            }

            TextClassificationModel<Long> model = trainer.train(examples)
                .orElseThrow(() -> new IllegalStateException("Unable to train entry category model with current data"));

            artifact.setStatus(EntryCategorySuggestionModelStatus.READY);
            artifact.setModelName(model.modelName());
            artifact.setPipelineVersion(model.pipelineVersion());
            artifact.setTrainingExampleCount(model.trainingExampleCount());
            artifact.setLabelCount(model.labelCount());
            artifact.setVocabularySize(model.vocabularySize());
            artifact.setModelPayload(serialize(model));
            artifact.setFailureMessage(null);
            artifact.setCompletedAt(LocalDateTime.now());
            EntryCategorySuggestionModelArtifact savedArtifact = modelRepository.save(artifact);
            return new EntryCategoryModelSnapshot(savedArtifact, model);
        } catch (Exception ex) {
            artifact.setStatus(EntryCategorySuggestionModelStatus.FAILED);
            artifact.setFailureMessage(truncateFailure(ex.getMessage()));
            artifact.setCompletedAt(LocalDateTime.now());
            modelRepository.save(artifact);
            throw new IllegalStateException("Unable to train entry category suggestion model", ex);
        }
    }

    /**
     * Reconstructs an in-memory model from the persisted artifact payload.
     *
     * @param artifact the persisted artifact to deserialize
     * @return the reconstructed model
     */
    public TextClassificationModel<Long> deserialize(EntryCategorySuggestionModelArtifact artifact) {
        byte[] payload = Base64.getDecoder().decode(artifact.getModelPayload());
        try (ByteArrayInputStream input = new ByteArrayInputStream(payload);
             ObjectInputStream objectInput = new ObjectInputStream(input)) {
            @SuppressWarnings("unchecked")
            TextClassificationModel<Long> model = (TextClassificationModel<Long>) objectInput.readObject();
            return model;
        } catch (IOException | ClassNotFoundException ex) {
            throw new IllegalStateException("Unable to deserialize entry category suggestion model", ex);
        }
    }

    private EntryCategorySuggestionModelArtifact startArtifact(EntryCategoryModelTrainingTrigger trigger) {
        EntryCategorySuggestionModelArtifact artifact = new EntryCategorySuggestionModelArtifact();
        artifact.setVersion(modelRepository.nextVersion());
        artifact.setStatus(EntryCategorySuggestionModelStatus.TRAINING);
        artifact.setTriggerSource(trigger);
        artifact.setModelName("pending-training");
        artifact.setPipelineVersion("pending-training");
        artifact.setTrainingStartedAt(LocalDateTime.now());
        return modelRepository.save(artifact);
    }

    private String serialize(TextClassificationModel<Long> model) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ObjectOutputStream objectOutput = new ObjectOutputStream(output)) {
            objectOutput.writeObject(model);
            objectOutput.flush();
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to serialize entry category suggestion model", ex);
        }
    }

    private String truncateFailure(String message) {
        if (message == null || message.isBlank()) {
            return "Training failed without a detailed message";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
