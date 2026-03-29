package dev.ccosta.aisha.infrastructure.persistence.entry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.entry.EntryCategorySuggestionStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class EntryRepositoryAdapterTest {

    @Mock
    private JpaEntryRepository jpaEntryRepository;

    @InjectMocks
    private EntryRepositoryAdapter entryRepositoryAdapter;

    @Test
    void shouldNormalizeDescriptionFilterBeforeQuerying() {
        when(jpaEntryRepository.searchBySettlementDateBetweenAndFilters(
            any(),
            any(),
            anyLong(),
            anyLong(),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(EntryCategorySuggestionStatus.class),
            anyString(),
            anyString(),
            any(Pageable.class)
        )).thenReturn(Page.empty());

        entryRepositoryAdapter.listMostRecentBySettlementDateBetweenAndFilters(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            1L,
            2L,
            "Café_100%",
            false,
            false,
            0,
            25
        );

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        verify(jpaEntryRepository).searchBySettlementDateBetweenAndFilters(
            any(),
            any(),
            anyLong(),
            anyLong(),
            descriptionCaptor.capture(),
            anyBoolean(),
            anyBoolean(),
            any(EntryCategorySuggestionStatus.class),
            anyString(),
            anyString(),
            any(Pageable.class)
        );

        org.assertj.core.api.Assertions.assertThat(descriptionCaptor.getValue()).isEqualTo("CAFE\\_100\\%");
    }
}
