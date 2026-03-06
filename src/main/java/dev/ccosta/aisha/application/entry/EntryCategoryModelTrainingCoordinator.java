package dev.ccosta.aisha.application.entry;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Coordinates asynchronous model training so only one run happens at a time while concurrent requests are coalesced.
 */
@Component
public class EntryCategoryModelTrainingCoordinator {

    private static final Logger log = LoggerFactory.getLogger(EntryCategoryModelTrainingCoordinator.class);

    private final AtomicBoolean trainingInProgress = new AtomicBoolean(false);
    private final AtomicReference<EntryCategoryModelTrainingTrigger> pendingTrigger = new AtomicReference<>();
    private final TaskExecutor taskExecutor;
    private final EntryCategoryModelTrainingService trainingService;
    private final EntryCategoryModelManager modelManager;

    public EntryCategoryModelTrainingCoordinator(
        TaskExecutor taskExecutor,
        EntryCategoryModelTrainingService trainingService,
        EntryCategoryModelManager modelManager
    ) {
        this.taskExecutor = taskExecutor;
        this.trainingService = trainingService;
        this.modelManager = modelManager;
    }

    /**
     * Requests an asynchronous training cycle for the provided trigger.
     *
     * @param trigger the business event requesting retraining
     */
    public void requestTraining(EntryCategoryModelTrainingTrigger trigger) {
        pendingTrigger.set(trigger);
        scheduleIfNeeded();
    }

    /**
     * Returns whether a training cycle is currently running.
     *
     * @return true when training is active
     */
    public boolean isTrainingInProgress() {
        return trainingInProgress.get();
    }

    /**
     * Returns whether a new trigger was queued while another training cycle is running.
     *
     * @return true when a retraining request is waiting to be processed
     */
    public boolean isRetrainQueued() {
        return pendingTrigger.get() != null && trainingInProgress.get();
    }

    private void scheduleIfNeeded() {
        if (trainingInProgress.compareAndSet(false, true)) {
            taskExecutor.execute(this::drainQueue);
        }
    }

    private void drainQueue() {
        try {
            EntryCategoryModelTrainingTrigger trigger;
            while ((trigger = pendingTrigger.getAndSet(null)) != null) {
                try {
                    EntryCategoryModelSnapshot snapshot = trainingService.trainAndPersist(trigger);
                    modelManager.putInCache(snapshot);
                } catch (Exception ex) {
                    log.error("Unable to train entry category suggestion model. trigger={}", trigger, ex);
                }
            }
        } finally {
            trainingInProgress.set(false);
            if (pendingTrigger.get() != null) {
                scheduleIfNeeded();
            }
        }
    }
}
