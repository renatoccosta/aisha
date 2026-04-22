package dev.ccosta.aisha.web.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.entry.EntryNotFoundException;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.application.entry.transfer.EntryTransferService;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ConcurrentModel;

@ExtendWith(MockitoExtension.class)
class EntryTransferControllerTest {

    @Mock
    private EntryService entryService;

    @Mock
    private AccountService accountService;

    @Mock
    private EntryTransferService entryTransferService;

    @Mock
    private EntryListingModelAssembler listingModelAssembler;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private EntryTransferController entryTransferController;

    @Test
    void shouldLinkTransferFromListingSelectionAndReturnSuccessToast() {
        ConcurrentModel model = new ConcurrentModel();
        when(messageSource.getMessage(eq("entries.list.toast.transfer.link.success"), eq(null), any()))
            .thenReturn("Transferência associada com sucesso.");

        String view = entryTransferController.linkTransferFromSelection(
            1L,
            List.of(1L, 2L),
            baseDateFilter(),
            null,
            null,
            null,
            false,
            2,
            25,
            htmxRequest(),
            model
        );

        assertThat(view).isEqualTo("entries/list :: listing");
        assertThat(model.getAttribute("toastMessage")).isEqualTo("Transferência associada com sucesso.");
        assertThat(model.getAttribute("toastLevel")).isEqualTo("success");
        verify(entryTransferService).linkExistingEntries(1L, 2L);
    }

    @Test
    void shouldReturnErrorToastWhenNoCounterpartIsSelected() {
        ConcurrentModel model = new ConcurrentModel();
        when(messageSource.getMessage(eq("entries.list.toast.transfer.link.selection.single"), eq(null), any()))
            .thenReturn("Selecione exatamente um outro lançamento para associar como transferência.");

        String view = entryTransferController.linkTransferFromSelection(
            1L,
            List.of(1L),
            baseDateFilter(),
            null,
            null,
            null,
            false,
            2,
            25,
            htmxRequest(),
            model
        );

        assertThat(view).isEqualTo("entries/list :: listing");
        assertThat(model.getAttribute("toastLevel")).isEqualTo("error");
        assertThat(model.getAttribute("toastMessage"))
            .isEqualTo("Selecione exatamente um outro lançamento para associar como transferência.");
    }

    @Test
    void shouldReturnErrorToastWhenMoreThanOneCounterpartIsSelected() {
        ConcurrentModel model = new ConcurrentModel();
        when(messageSource.getMessage(eq("entries.list.toast.transfer.link.selection.single"), eq(null), any()))
            .thenReturn("Selecione exatamente um outro lançamento para associar como transferência.");

        String view = entryTransferController.linkTransferFromSelection(
            1L,
            List.of(1L, 2L, 3L),
            baseDateFilter(),
            null,
            null,
            null,
            false,
            2,
            25,
            htmxRequest(),
            model
        );

        assertThat(view).isEqualTo("entries/list :: listing");
        assertThat(model.getAttribute("toastLevel")).isEqualTo("error");
        assertThat(model.getAttribute("toastMessage"))
            .isEqualTo("Selecione exatamente um outro lançamento para associar como transferência.");
    }

    @Test
    void shouldReturnExplainedErrorToastWhenSelectedCounterpartIsIncompatible() {
        ConcurrentModel model = new ConcurrentModel();
        when(messageSource.getMessage(eq("entries.list.toast.transfer.link.incompatible"), any(), any()))
            .thenReturn("O lançamento selecionado não pode ser associado: os lançamentos devem ter sinais opostos");
        when(entryTransferService.linkExistingEntries(1L, 2L))
            .thenThrow(new IllegalArgumentException("Transfer entries must have opposite signs"));

        String view = entryTransferController.linkTransferFromSelection(
            1L,
            List.of(1L, 2L),
            baseDateFilter(),
            null,
            null,
            null,
            false,
            2,
            25,
            htmxRequest(),
            model
        );

        assertThat(view).isEqualTo("entries/list :: listing");
        assertThat(model.getAttribute("toastLevel")).isEqualTo("error");
        assertThat(model.getAttribute("toastMessage"))
            .isEqualTo("O lançamento selecionado não pode ser associado: os lançamentos devem ter sinais opostos");
    }

    @Test
    void shouldReturnExplainedErrorToastWhenSelectedCounterpartIsMissing() {
        ConcurrentModel model = new ConcurrentModel();
        when(messageSource.getMessage(eq("entries.list.toast.transfer.link.incompatible"), any(), any()))
            .thenReturn("O lançamento selecionado não pode ser associado: o lançamento selecionado não foi encontrado");
        when(entryTransferService.linkExistingEntries(1L, 2L))
            .thenThrow(new EntryNotFoundException(2L));

        String view = entryTransferController.linkTransferFromSelection(
            1L,
            List.of(1L, 2L),
            baseDateFilter(),
            null,
            null,
            null,
            false,
            2,
            25,
            htmxRequest(),
            model
        );

        assertThat(view).isEqualTo("entries/list :: listing");
        assertThat(model.getAttribute("toastLevel")).isEqualTo("error");
        assertThat(model.getAttribute("toastMessage"))
            .isEqualTo("O lançamento selecionado não pode ser associado: o lançamento selecionado não foi encontrado");
    }

    private DateFilterState baseDateFilter() {
        DateFilterState state = new DateFilterState();
        state.applyCustom(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        return state;
    }

    private MockHttpServletRequest htmxRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("HX-Request", "true");
        return request;
    }
}
