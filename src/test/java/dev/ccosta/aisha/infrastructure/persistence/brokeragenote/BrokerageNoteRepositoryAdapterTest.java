package dev.ccosta.aisha.infrastructure.persistence.brokeragenote;

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
class BrokerageNoteRepositoryAdapterTest {

    @Mock
    private JpaBrokerageNoteRepository jpaBrokerageNoteRepository;

    @InjectMocks
    private BrokerageNoteRepositoryAdapter brokerageNoteRepositoryAdapter;

    @Test
    @SuppressWarnings("unchecked")
    void shouldQueryUsingSpecification() {
        when(jpaBrokerageNoteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

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

        verify(jpaBrokerageNoteRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldQueryUsingSpecificationWhenFilterIsBlank() {
        when(jpaBrokerageNoteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

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

        verify(jpaBrokerageNoteRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
