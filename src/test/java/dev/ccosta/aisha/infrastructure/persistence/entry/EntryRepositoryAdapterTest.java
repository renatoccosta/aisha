package dev.ccosta.aisha.infrastructure.persistence.entry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class EntryRepositoryAdapterTest {

    @Mock
    private JpaEntryRepository jpaEntryRepository;

    @InjectMocks
    private EntryRepositoryAdapter entryRepositoryAdapter;

    @Test
    @SuppressWarnings("unchecked")
    void shouldQueryUsingSpecification() {
        when(jpaEntryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

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

        verify(jpaEntryRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldQueryUsingSpecificationWhenFilterIsBlank() {
        when(jpaEntryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        entryRepositoryAdapter.listMostRecentBySettlementDateBetweenAndFilters(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            1L,
            2L,
            "   ",
            false,
            false,
            0,
            25
        );

        verify(jpaEntryRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
