package dev.ccosta.aisha.web.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.domain.account.Account;
import java.time.LocalDate;
import java.util.Optional;
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

    @Test
    void shouldStartPdfImportJob() {
        Account account = new Account();
        when(accountService.findById(10L)).thenReturn(account);
        BrokerageNoteImportJobCoordinator coordinator = new BrokerageNoteImportJobCoordinator(accountService, new SyncTaskExecutor());
        MockMultipartFile file = new MockMultipartFile("file", "nota.pdf", "application/pdf", new byte[] {1, 2, 3});

        String jobId = coordinator.startJob(file, 10L);
        BrokerageNoteImportJobSnapshot snapshot = coordinator.getSnapshot(jobId);

        assertThat(snapshot.status()).isEqualTo(BrokerageNoteImportJobStatus.SUCCESS);
        assertThat(snapshot.summary().importedNotes()).isZero();
        assertThat(snapshot.summary().importedOperations()).isZero();
    }

    @Test
    void shouldRejectNonPdfFile() {
        Account account = new Account();
        when(accountService.findById(10L)).thenReturn(account);
        BrokerageNoteImportJobCoordinator coordinator = new BrokerageNoteImportJobCoordinator(accountService, new SyncTaskExecutor());
        MockMultipartFile file = new MockMultipartFile("file", "nota.txt", "text/plain", new byte[] {1});

        assertThatThrownBy(() -> coordinator.startJob(file, 10L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Only PDF files are accepted");
    }

    @Test
    void shouldRejectInactiveAccount() {
        Account account = new Account();
        account.setDeactivationDate(LocalDate.of(2026, 4, 1));
        when(accountService.findById(10L)).thenReturn(account);
        BrokerageNoteImportJobCoordinator coordinator = new BrokerageNoteImportJobCoordinator(accountService, new SyncTaskExecutor());
        MockMultipartFile file = new MockMultipartFile("file", "nota.pdf", "application/pdf", new byte[] {1});

        assertThatThrownBy(() -> coordinator.startJob(file, 10L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Selected account must be active");
    }
}
