package dev.ccosta.aisha.application.brokeragenote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.brokeragenote.BrokerageNote;
import dev.ccosta.aisha.domain.brokeragenote.BrokerageNoteRepository;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrokerageNoteServiceTest {

    @Mock
    private BrokerageNoteRepository brokerageNoteRepository;

    @InjectMocks
    private BrokerageNoteService brokerageNoteService;

    @Test
    void shouldListBrokerageNotesInsideSettlementRange() {
        LocalDate settlementStartDate = LocalDate.of(2026, 4, 1);
        LocalDate settlementEndDate = LocalDate.of(2026, 4, 30);
        LocalDate tradeStartDate = LocalDate.of(2026, 4, 5);
        LocalDate tradeEndDate = LocalDate.of(2026, 4, 20);
        PagedResult<BrokerageNote> page = new PagedResult<>(List.of(), 0, 25, 0, 0);
        when(brokerageNoteRepository.findPageOrdered(
            settlementStartDate,
            settlementEndDate,
            10L,
            tradeStartDate,
            tradeEndDate,
            "123",
            0,
            25
        )).thenReturn(page);

        PagedResult<BrokerageNote> result = brokerageNoteService.listPageOrdered(
            settlementStartDate,
            settlementEndDate,
            10L,
            tradeStartDate,
            tradeEndDate,
            "123",
            0,
            25
        );

        assertThat(result).isSameAs(page);
        verify(brokerageNoteRepository).findPageOrdered(
            settlementStartDate,
            settlementEndDate,
            10L,
            tradeStartDate,
            tradeEndDate,
            "123",
            0,
            25
        );
    }

    @Test
    void shouldRejectInvalidSettlementRange() {
        assertThatThrownBy(() -> brokerageNoteService.listPageOrdered(
            LocalDate.of(2026, 4, 30),
            LocalDate.of(2026, 4, 1),
            null,
            null,
            null,
            null,
            0,
            25
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Settlement end date must be greater than or equal to start date");
    }

    @Test
    void shouldFindById() {
        BrokerageNote note = new BrokerageNote();
        when(brokerageNoteRepository.findById(50L)).thenReturn(Optional.of(note));

        BrokerageNote result = brokerageNoteService.findById(50L);

        assertThat(result).isSameAs(note);
    }
}
