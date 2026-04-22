package dev.ccosta.aisha.web.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.entry.categorization.model.EntryCategoryModelTrainingCoordinator;
import dev.ccosta.aisha.application.entry.categorization.model.EntryCategoryModelTrainingTrigger;
import dev.ccosta.aisha.application.entry.importing.EntryCsvImportOptions;
import dev.ccosta.aisha.application.entry.importing.EntryCsvImportService;
import dev.ccosta.aisha.application.entry.importing.EntryImportSummary;
import dev.ccosta.aisha.application.entry.importing.statement.EntryStatementImportService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class EntryImportJobCoordinatorTest {

    @Mock
    private EntryCsvImportService entryCsvImportService;

    @Mock
    private EntryStatementImportService entryStatementImportService;

    @Mock
    private EntryCategoryModelTrainingCoordinator modelTrainingCoordinator;

    @Test
    void shouldRequestRetrainingAfterSuccessfulCsvImport() {
        TaskExecutor sameThreadExecutor = Runnable::run;
        EntryImportJobCoordinator coordinator = new EntryImportJobCoordinator(
            entryCsvImportService,
            entryStatementImportService,
            modelTrainingCoordinator,
            sameThreadExecutor
        );
        when(entryCsvImportService.importCsv(any(), any(), any())).thenReturn(new EntryImportSummary(2, 0, 0, 0, 5));

        String jobId = coordinator.startCsvJob(
            new MockMultipartFile("file", "entries.csv", "text/csv", "a,b,c".getBytes(StandardCharsets.UTF_8)),
            new EntryCsvImportOptions(',', "uuuu-MM-dd", "^-?\\d+$", true)
        );

        assertThat(coordinator.getSnapshot(jobId)).isNotNull();
        verify(modelTrainingCoordinator).requestTraining(EntryCategoryModelTrainingTrigger.CSV_IMPORT);
    }

    @Test
    void shouldNotRequestRetrainingAfterStatementImport() {
        TaskExecutor sameThreadExecutor = Runnable::run;
        EntryImportJobCoordinator coordinator = new EntryImportJobCoordinator(
            entryCsvImportService,
            entryStatementImportService,
            modelTrainingCoordinator,
            sameThreadExecutor
        );
        when(entryStatementImportService.importStatement(any(), any(), any(), any())).thenReturn(new EntryImportSummary(1, 0, 0, 0, 3));

        String jobId = coordinator.startStatementJob(
            new MockMultipartFile("file", "statement.ofx", "application/octet-stream", "data".getBytes(StandardCharsets.UTF_8)),
            1L,
            "ofx"
        );

        assertThat(coordinator.getSnapshot(jobId)).isNotNull();
        verify(modelTrainingCoordinator, never()).requestTraining(any());
    }
}
