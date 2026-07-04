package dev.ccosta.aisha.web.brokeragenote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.brokeragenote.BrokerageNoteService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.brokeragenote.BrokerageNote;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
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
        MockHttpServletRequest request = htmxRequest();
        request.setQueryString("accountId=10&tradeStartDate=2026-04-05&tradeEndDate=2026-04-20&noteNumber=123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String view = brokerageNoteController.table(
            globalDateFilter,
            10L,
            tradeStartDate,
            tradeEndDate,
            " 123 ",
            null,
            null,
            request,
            response,
            model
        );

        assertThat(view).isEqualTo("investments/brokerage-notes/list :: table");
        assertThat(response.getHeader("HX-Push-Url"))
            .isEqualTo("/investments/brokerage-notes?accountId=10&tradeStartDate=2026-04-05&tradeEndDate=2026-04-20&noteNumber=123");
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
        BrokerageNote note = brokerageNoteWithNetEntry(50L, 60L);
        when(brokerageNoteService.findById(50L)).thenReturn(note);

        ConcurrentModel model = new ConcurrentModel();
        String view = brokerageNoteController.details(50L, "/investments/brokerage-notes?page=1", model);

        assertThat(view).isEqualTo("investments/brokerage-notes/details");
        assertThat(model.getAttribute("brokerageNote")).isSameAs(note);
        assertThat(model.getAttribute("netEntryId")).isEqualTo(60L);
        assertThat(model.getAttribute("brokerageNoteDetailsReturnPath")).isEqualTo("/investments/brokerage-notes/50");
        assertThat(model.getAttribute("returnTo")).isEqualTo("/investments/brokerage-notes?page=1");
    }

    private DateFilterState baseDateFilter() {
        DateFilterState state = new DateFilterState();
        state.applyCustom(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        return state;
    }

    private BrokerageNote brokerageNoteWithNetEntry(Long noteId, Long entryId) {
        Entry entry = new Entry();
        ReflectionTestUtils.setField(entry, "id", entryId);
        BrokerageNote note = new BrokerageNote();
        ReflectionTestUtils.setField(note, "id", noteId);
        note.setNetEntry(entry);
        return note;
    }

    private MockHttpServletRequest htmxRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("HX-Request", "true");
        return request;
    }
}
