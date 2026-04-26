package dev.ccosta.aisha.web.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.investment.importing.BrokerageNoteImportService;
import dev.ccosta.aisha.application.investment.importing.BrokerageNoteImportSummary;
import dev.ccosta.aisha.application.investment.importing.UnsupportedBrokerageNoteFormatException;
import dev.ccosta.aisha.domain.account.Account;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class BrokerageNoteImportJobCoordinatorTest {

    @Mock
    private AccountService accountService;

    @Mock
    private BrokerageNoteImportService importService;

    @Test
    void shouldStartImportJob() {
        Account account = new Account();
        when(accountService.findById(10L)).thenReturn(account);
        MockMultipartFile file = new MockMultipartFile("file", "nota.pdf", "application/pdf", new byte[] {1, 2, 3});
        when(importService.importFile(
            eq(10L),
            eq("nota.pdf"),
            eq("039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81"),
            any(byte[].class)
        ))
            .thenReturn(new BrokerageNoteImportSummary(0, 0, 0, 10));
        BrokerageNoteImportJobCoordinator coordinator = new BrokerageNoteImportJobCoordinator(accountService, importService, new SyncTaskExecutor());

        String jobId = coordinator.startJob(file, 10L);
        BrokerageNoteImportJobSnapshot snapshot = coordinator.getSnapshot(jobId);

        assertThat(snapshot.status()).isEqualTo(BrokerageNoteImportJobStatus.SUCCESS);
        assertThat(snapshot.summary().importedNotes()).isZero();
        assertThat(snapshot.summary().importedOperations()).isZero();
    }

    @Test
    void shouldMarkJobAsFailedWhenFormatIsUnsupported() {
        Account account = new Account();
        when(accountService.findById(10L)).thenReturn(account);
        BrokerageNoteImportJobCoordinator coordinator = new BrokerageNoteImportJobCoordinator(accountService, importService, new SyncTaskExecutor());
        MockMultipartFile file = new MockMultipartFile("file", "nota.txt", "text/plain", new byte[] {1});
        when(importService.importFile(eq(10L), eq("nota.txt"), any(String.class), any(byte[].class)))
            .thenThrow(new UnsupportedBrokerageNoteFormatException("nota.txt"));

        String jobId = coordinator.startJob(file, 10L);
        BrokerageNoteImportJobSnapshot snapshot = coordinator.getSnapshot(jobId);

        assertThat(snapshot.status()).isEqualTo(BrokerageNoteImportJobStatus.FAILED);
        assertThat(snapshot.failureMessage()).isEqualTo("Unsupported brokerage note file format: nota.txt");
    }

    @Test
    void shouldRejectInactiveAccount() {
        Account account = new Account();
        account.setDeactivationDate(LocalDate.of(2026, 4, 1));
        when(accountService.findById(10L)).thenReturn(account);
        BrokerageNoteImportJobCoordinator coordinator = new BrokerageNoteImportJobCoordinator(accountService, importService, new SyncTaskExecutor());
        MockMultipartFile file = new MockMultipartFile("file", "nota.pdf", "application/pdf", new byte[] {1});

        assertThatThrownBy(() -> coordinator.startJob(file, 10L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Selected account must be active");
    }
}
