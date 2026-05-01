package dev.ccosta.aisha.web.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.investment.BrokerageNoteService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.investment.BrokerageNote;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;

@ExtendWith(MockitoExtension.class)
class BrokerageNoteControllerTest {

    @Mock
    private BrokerageNoteService brokerageNoteService;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private BrokerageNoteController brokerageNoteController;

    @Test
    void shouldApplyListingFiltersAndGlobalSettlementDateFilter() {
        DateFilterState globalDateFilter = baseDateFilter();
        LocalDate tradeStartDate = LocalDate.of(2026, 4, 5);
        LocalDate tradeEndDate = LocalDate.of(2026, 4, 20);
        Account account = new Account();
        BrokerageNote note = new BrokerageNote();
        when(brokerageNoteService.listPageOrdered(
            globalDateFilter.getStartDate(),
            globalDateFilter.getEndDate(),
            10L,
            tradeStartDate,
            tradeEndDate,
            "123",
            0,
            25
        )).thenReturn(new PagedResult<>(List.of(note), 0, 25, 1, 1));
        when(accountService.listVisibleForEntryFilter(globalDateFilter.getStartDate())).thenReturn(List.of(account));

        ConcurrentModel model = new ConcurrentModel();
        String view = brokerageNoteController.table(
            globalDateFilter,
            10L,
            tradeStartDate,
            tradeEndDate,
            " 123 ",
            null,
            null,
            model
        );

        assertThat(view).isEqualTo("investments/brokerage-notes/list :: table");
        assertThat(model.getAttribute("brokerageNotes")).isEqualTo(List.of(note));
        assertThat(model.getAttribute("selectedAccountId")).isEqualTo(10L);
        assertThat(model.getAttribute("selectedNoteNumber")).isEqualTo("123");
        verify(brokerageNoteService).listPageOrdered(
            globalDateFilter.getStartDate(),
            globalDateFilter.getEndDate(),
            10L,
            tradeStartDate,
            tradeEndDate,
            "123",
            0,
            25
        );
    }

    @Test
    void shouldShowDetails() {
        BrokerageNote note = new BrokerageNote();
        when(brokerageNoteService.findById(50L)).thenReturn(note);

        ConcurrentModel model = new ConcurrentModel();
        String view = brokerageNoteController.details(50L, "/investments/brokerage-notes?page=1", model);

        assertThat(view).isEqualTo("investments/brokerage-notes/details");
        assertThat(model.getAttribute("brokerageNote")).isSameAs(note);
        assertThat(model.getAttribute("returnTo")).isEqualTo("/investments/brokerage-notes?page=1");
    }

    private DateFilterState baseDateFilter() {
        DateFilterState state = new DateFilterState();
        state.applyCustom(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        return state;
    }
}
