package dev.ccosta.aisha.application.entry.categorization.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.ai.classification.TextClassificationRequest;
import dev.ccosta.aisha.domain.entry.categorization.model.EntryCategorySuggestionModelRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class EntryCategoryModelManagerTest {

    @Mock
    private EntryCategorySuggestionModelRepository modelRepository;

    @Mock
    private EntryCategoryModelTrainingService trainingService;

    @Mock
    private EntryCategoryModelTrainingCoordinator trainingCoordinator;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldRequestInitialTrainingImmediatelyWhenNoTransactionIsActive() {
        EntryCategoryModelManager manager = createManager();
        when(modelRepository.findLatestReady()).thenReturn(Optional.empty());
        when(modelRepository.findLatest()).thenReturn(Optional.empty());

        Optional<?> prediction = manager.predict(new TextClassificationRequest("mercado", java.util.List.of("account-1")));

        assertThat(prediction).isEmpty();
        verify(trainingCoordinator).requestTraining(EntryCategoryModelTrainingTrigger.INITIAL);
    }

    @Test
    void shouldDeferInitialTrainingUntilTransactionCommit() {
        EntryCategoryModelManager manager = createManager();
        when(modelRepository.findLatestReady()).thenReturn(Optional.empty());
        when(modelRepository.findLatest()).thenReturn(Optional.empty());
        TransactionSynchronizationManager.initSynchronization();

        Optional<?> prediction = manager.predict(new TextClassificationRequest("mercado", java.util.List.of("account-1")));

        assertThat(prediction).isEmpty();
        verify(trainingCoordinator, never()).requestTraining(any());

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
            synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        }

        verify(trainingCoordinator).requestTraining(EntryCategoryModelTrainingTrigger.INITIAL);
    }

    @Test
    void shouldRegisterOnlyOneDeferredTrainingRequestPerTransaction() {
        EntryCategoryModelManager manager = createManager();
        when(modelRepository.findLatestReady()).thenReturn(Optional.empty());
        when(modelRepository.findLatest()).thenReturn(Optional.empty());
        TransactionSynchronizationManager.initSynchronization();

        manager.predict(new TextClassificationRequest("mercado", java.util.List.of("account-1")));
        manager.predict(new TextClassificationRequest("farmacia", java.util.List.of("account-1")));

        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        verify(trainingCoordinator, never()).requestTraining(any());
    }

    private EntryCategoryModelManager createManager() {
        ObjectProvider<EntryCategoryModelTrainingCoordinator> coordinatorProvider = new ObjectProvider<>() {
            @Override
            public EntryCategoryModelTrainingCoordinator getObject(Object... args) {
                return trainingCoordinator;
            }

            @Override
            public EntryCategoryModelTrainingCoordinator getIfAvailable() {
                return trainingCoordinator;
            }

            @Override
            public EntryCategoryModelTrainingCoordinator getIfUnique() {
                return trainingCoordinator;
            }

            @Override
            public EntryCategoryModelTrainingCoordinator getObject() {
                return trainingCoordinator;
            }
        };
        return new EntryCategoryModelManager(modelRepository, trainingService, coordinatorProvider);
    }
}
