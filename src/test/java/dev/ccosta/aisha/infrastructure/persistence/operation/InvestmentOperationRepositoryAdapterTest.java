package dev.ccosta.aisha.infrastructure.persistence.operation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.operation.InvestmentOperationType;
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
class InvestmentOperationRepositoryAdapterTest {

    @Mock
    private JpaInvestmentOperationRepository jpaInvestmentOperationRepository;

    @InjectMocks
    private InvestmentOperationRepositoryAdapter investmentOperationRepositoryAdapter;

    @Test
    @SuppressWarnings("unchecked")
    void shouldQueryUsingSpecification() {
        when(jpaInvestmentOperationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        investmentOperationRepositoryAdapter.findPageOrdered(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            "Ação_100%",
            1L,
            InvestmentOperationType.BUY,
            null,
            0,
            25
        );

        verify(jpaInvestmentOperationRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldQueryUsingSpecificationWhenFilterIsBlank() {
        when(jpaInvestmentOperationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        investmentOperationRepositoryAdapter.findPageOrdered(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            "   ",
            1L,
            InvestmentOperationType.BUY,
            null,
            0,
            25
        );

        verify(jpaInvestmentOperationRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
