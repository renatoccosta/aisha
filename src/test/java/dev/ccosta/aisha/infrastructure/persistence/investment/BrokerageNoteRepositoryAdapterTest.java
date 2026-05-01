package dev.ccosta.aisha.infrastructure.persistence.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class BrokerageNoteRepositoryAdapterTest {

    @Mock
    private JpaBrokerageNoteRepository jpaBrokerageNoteRepository;

    @InjectMocks
    private BrokerageNoteRepositoryAdapter brokerageNoteRepositoryAdapter;

    @Test
    void shouldNormalizeNoteNumberPrefixBeforeQuerying() {
        when(jpaBrokerageNoteRepository.searchByFilters(
            any(LocalDate.class),
            any(LocalDate.class),
            nullable(Long.class),
            nullable(LocalDate.class),
            nullable(LocalDate.class),
            anyString(),
            any(Pageable.class)
        )).thenReturn(Page.empty());

        brokerageNoteRepositoryAdapter.findPageOrdered(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            1L,
            null,
            null,
            "nt_100%",
            0,
            25
        );

        ArgumentCaptor<String> noteNumberCaptor = ArgumentCaptor.forClass(String.class);
        verify(jpaBrokerageNoteRepository).searchByFilters(
            any(LocalDate.class),
            any(LocalDate.class),
            nullable(Long.class),
            nullable(LocalDate.class),
            nullable(LocalDate.class),
            noteNumberCaptor.capture(),
            any(Pageable.class)
        );

        assertThat(noteNumberCaptor.getValue()).isEqualTo("NT\\_100\\%");
    }

    @Test
    void shouldUseQueryWithoutNoteNumberWhenFilterIsBlank() {
        when(jpaBrokerageNoteRepository.searchByFiltersWithoutNoteNumber(
            any(LocalDate.class),
            any(LocalDate.class),
            nullable(Long.class),
            nullable(LocalDate.class),
            nullable(LocalDate.class),
            any(Pageable.class)
        )).thenReturn(Page.empty());

        brokerageNoteRepositoryAdapter.findPageOrdered(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            1L,
            null,
            null,
            "   ",
            0,
            25
        );

        verify(jpaBrokerageNoteRepository).searchByFiltersWithoutNoteNumber(
            any(LocalDate.class),
            any(LocalDate.class),
            nullable(Long.class),
            nullable(LocalDate.class),
            nullable(LocalDate.class),
            any(Pageable.class)
        );
        verify(jpaBrokerageNoteRepository, never()).searchByFilters(
            any(LocalDate.class),
            any(LocalDate.class),
            nullable(Long.class),
            nullable(LocalDate.class),
            nullable(LocalDate.class),
            anyString(),
            any(Pageable.class)
        );
    }
}
