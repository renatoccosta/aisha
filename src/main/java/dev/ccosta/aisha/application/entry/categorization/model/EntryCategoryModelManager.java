package dev.ccosta.aisha.application.entry.categorization.model;

import dev.ccosta.aisha.application.ai.classification.ClassificationPrediction;
import dev.ccosta.aisha.application.ai.classification.TextClassificationModel;
import dev.ccosta.aisha.application.ai.classification.TextClassificationRequest;
import dev.ccosta.aisha.domain.entry.categorization.model.EntryCategorySuggestionModelArtifact;
import dev.ccosta.aisha.domain.entry.categorization.model.EntryCategorySuggestionModelRepository;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Provides cached access to the latest ready entry category suggestion model.
 */
@Service
public class EntryCategoryModelManager {

    private static final Object INITIAL_TRAINING_REGISTRATION_KEY = new Object();

    private final AtomicReference<EntryCategoryModelSnapshot> cachedSnapshot = new AtomicReference<>();
    private final EntryCategorySuggestionModelRepository modelRepository;
    private final EntryCategoryModelTrainingService trainingService;
    private final ObjectProvider<EntryCategoryModelTrainingCoordinator> trainingCoordinatorProvider;

    public EntryCategoryModelManager(
        EntryCategorySuggestionModelRepository modelRepository,
        EntryCategoryModelTrainingService trainingService,
        ObjectProvider<EntryCategoryModelTrainingCoordinator> trainingCoordinatorProvider
    ) {
        this.modelRepository = modelRepository;
        this.trainingService = trainingService;
        this.trainingCoordinatorProvider = trainingCoordinatorProvider;
    }

    /**
     * Predicts a category using the current active model. When no model exists yet, an initial asynchronous training is requested.
     *
     * @param request the request to classify
     * @return the predicted category metadata, or empty when no active model is currently available
     */
    public Optional<ClassificationPrediction<Long>> predict(TextClassificationRequest request) {
        Optional<TextClassificationModel<Long>> model = loadActiveModel();
        if (model.isEmpty()) {
            if (modelRepository.findLatest().isEmpty()) {
                requestInitialTraining();
            }
            return Optional.empty();
        }
        return model.get().predict(request);
    }

    /**
     * Returns the operational status of the model lifecycle.
     *
     * @return the current model status for UI and operations
     */
    public EntryCategoryModelStatusView status() {
        EntryCategorySuggestionModelArtifact latest = modelRepository.findLatest().orElse(null);
        if (latest == null) {
            return new EntryCategoryModelStatusView(
                false,
                trainingCoordinator().isTrainingInProgress(),
                trainingCoordinator().isRetrainQueued(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
        }

        return new EntryCategoryModelStatusView(
            modelRepository.findLatestReady().isPresent(),
            trainingCoordinator().isTrainingInProgress(),
            trainingCoordinator().isRetrainQueued(),
            latest.getVersion(),
            latest.getStatus().name(),
            latest.getModelName(),
            latest.getPipelineVersion(),
            latest.getTrainingExampleCount(),
            latest.getLabelCount(),
            latest.getVocabularySize(),
            latest.getTriggerSource() == null ? null : latest.getTriggerSource().name(),
            latest.getCompletedAt(),
            latest.getFailureMessage()
        );
    }

    /**
     * Updates the in-memory cache after a training cycle successfully produced a ready model.
     *
     * @param snapshot the new model snapshot to expose for inference
     */
    public void putInCache(EntryCategoryModelSnapshot snapshot) {
        cachedSnapshot.set(snapshot);
    }

    private Optional<TextClassificationModel<Long>> loadActiveModel() {
        EntryCategoryModelSnapshot snapshot = cachedSnapshot.get();
        if (snapshot != null) {
            return Optional.of(snapshot.model());
        }

        Optional<EntryCategorySuggestionModelArtifact> latestReady = modelRepository.findLatestReady();
        if (latestReady.isEmpty()) {
            return Optional.empty();
        }

        EntryCategorySuggestionModelArtifact artifact = latestReady.get();
        TextClassificationModel<Long> model = trainingService.deserialize(artifact);
        cachedSnapshot.compareAndSet(null, new EntryCategoryModelSnapshot(artifact, model));
        EntryCategoryModelSnapshot loadedSnapshot = cachedSnapshot.get();
        return loadedSnapshot == null ? Optional.of(model) : Optional.of(loadedSnapshot.model());
    }

    private EntryCategoryModelTrainingCoordinator trainingCoordinator() {
        return trainingCoordinatorProvider.getObject();
    }

    private void requestInitialTraining() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            trainingCoordinator().requestTraining(EntryCategoryModelTrainingTrigger.INITIAL);
            return;
        }

        if (TransactionSynchronizationManager.hasResource(INITIAL_TRAINING_REGISTRATION_KEY)) {
            return;
        }

        TransactionSynchronizationManager.bindResource(INITIAL_TRAINING_REGISTRATION_KEY, Boolean.TRUE);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                trainingCoordinator().requestTraining(EntryCategoryModelTrainingTrigger.INITIAL);
            }

            @Override
            public void afterCompletion(int status) {
                if (TransactionSynchronizationManager.hasResource(INITIAL_TRAINING_REGISTRATION_KEY)) {
                    TransactionSynchronizationManager.unbindResource(INITIAL_TRAINING_REGISTRATION_KEY);
                }
            }
        });
    }
}
