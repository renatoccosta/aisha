package dev.ccosta.aisha.infrastructure.persistence.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
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
class InvestmentOperationRepositoryAdapterTest {

    @Mock
    private JpaInvestmentOperationRepository jpaInvestmentOperationRepository;

    @InjectMocks
    private InvestmentOperationRepositoryAdapter investmentOperationRepositoryAdapter;

    @Test
    void shouldNormalizeAssetFilterBeforeQuerying() {
        when(jpaInvestmentOperationRepository.searchByFilters(
            any(LocalDate.class),
            any(LocalDate.class),
            anyString(),
            nullable(Long.class),
            nullable(InvestmentOperationType.class),
            nullable(Long.class),
            any(Pageable.class)
        )).thenReturn(Page.empty());

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

        ArgumentCaptor<String> assetFilterCaptor = ArgumentCaptor.forClass(String.class);
        verify(jpaInvestmentOperationRepository).searchByFilters(
            any(LocalDate.class),
            any(LocalDate.class),
            assetFilterCaptor.capture(),
            nullable(Long.class),
            nullable(InvestmentOperationType.class),
            nullable(Long.class),
            any(Pageable.class)
        );

        assertThat(assetFilterCaptor.getValue()).isEqualTo("ACAO\\_100\\%");
    }

    @Test
    void shouldUseQueryWithoutAssetWhenFilterIsBlank() {
        when(jpaInvestmentOperationRepository.searchByFiltersWithoutAsset(
            any(LocalDate.class),
            any(LocalDate.class),
            nullable(Long.class),
            nullable(InvestmentOperationType.class),
            nullable(Long.class),
            any(Pageable.class)
        )).thenReturn(Page.empty());

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

        verify(jpaInvestmentOperationRepository).searchByFiltersWithoutAsset(
            any(LocalDate.class),
            any(LocalDate.class),
            nullable(Long.class),
            nullable(InvestmentOperationType.class),
            nullable(Long.class),
            any(Pageable.class)
        );
        verify(jpaInvestmentOperationRepository, never()).searchByFilters(
            any(LocalDate.class),
            any(LocalDate.class),
            anyString(),
            nullable(Long.class),
            nullable(InvestmentOperationType.class),
            nullable(Long.class),
            any(Pageable.class)
        );
    }
}
