package dev.ccosta.aisha.web.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ConcurrentModel;

@ExtendWith(MockitoExtension.class)
class BrokerageNoteImportControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private BrokerageNoteImportJobCoordinator jobCoordinator;

    @InjectMocks
    private BrokerageNoteImportController controller;

    @Test
    void shouldRenderImportPageWithAccountOptions() {
        DateFilterState dateFilter = baseDateFilter();
        Account account = new Account();
        when(accountService.listVisibleForEntryFilter(dateFilter.getStartDate())).thenReturn(List.of(account));

        ConcurrentModel model = new ConcurrentModel();
        String view = controller.importPage(dateFilter, model);

        assertThat(view).isEqualTo("investments/brokerage-note-import");
        assertThat(model.getAttribute("mode")).isEqualTo("idle");
        assertThat(model.getAttribute("brokerageNoteAccountOptions")).isEqualTo(List.of(account));
    }

    @Test
    void shouldStartImportJobAndReturnResultFragment() {
        DateFilterState dateFilter = baseDateFilter();
        MockMultipartFile file = new MockMultipartFile("file", "nota.pdf", "application/pdf", new byte[] {1});
        BrokerageNoteImportJobSnapshot snapshot = new BrokerageNoteImportJobSnapshot(
            "job-1",
            BrokerageNoteImportJobStatus.PROCESSING,
            1,
            0,
            null,
            null
        );
        when(accountService.listVisibleForEntryFilter(dateFilter.getStartDate())).thenReturn(List.of());
        when(jobCoordinator.startJob(file, 10L)).thenReturn("job-1");
        when(jobCoordinator.getSnapshot("job-1")).thenReturn(snapshot);

        ConcurrentModel model = new ConcurrentModel();
        String view = controller.startImport(file, 10L, dateFilter, model);

        assertThat(view).isEqualTo("investments/brokerage-note-import :: result");
        assertThat(model.getAttribute("mode")).isEqualTo("processing");
        assertThat(model.getAttribute("jobId")).isEqualTo("job-1");
        verify(jobCoordinator).startJob(file, 10L);
    }

    @Test
    void shouldReturnFailureFragmentWhenStartValidationFails() {
        DateFilterState dateFilter = baseDateFilter();
        when(accountService.listVisibleForEntryFilter(dateFilter.getStartDate())).thenReturn(List.of());
        when(jobCoordinator.startJob(null, null)).thenThrow(new IllegalArgumentException("Account must be informed"));

        ConcurrentModel model = new ConcurrentModel();
        String view = controller.startImport(null, null, dateFilter, model);

        assertThat(view).isEqualTo("investments/brokerage-note-import :: result");
        assertThat(model.getAttribute("mode")).isEqualTo("failed");
        assertThat(model.getAttribute("failureCauseKey")).isEqualTo("investments.brokerageNoteImport.result.failure.missingAccount");
    }

    private DateFilterState baseDateFilter() {
        DateFilterState state = new DateFilterState();
        state.applyCustom(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        return state;
    }
}
