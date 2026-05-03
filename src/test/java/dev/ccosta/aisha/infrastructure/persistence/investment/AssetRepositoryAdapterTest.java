package dev.ccosta.aisha.infrastructure.persistence.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.investment.AssetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AssetRepositoryAdapterTest {

    @Mock
    private JpaAssetRepository jpaAssetRepository;

    @InjectMocks
    private AssetRepositoryAdapter assetRepositoryAdapter;

    @Test
    void shouldNormalizeDescriptionFilterBeforeQuerying() {
        when(jpaAssetRepository.searchByFilters(nullable(AssetType.class), anyString(), any(Pageable.class)))
            .thenReturn(Page.empty());

        assetRepositoryAdapter.findPageOrdered(AssetType.STOCK, "Ação_100%", 0, 25);

        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        verify(jpaAssetRepository).searchByFilters(
            nullable(AssetType.class),
            descriptionCaptor.capture(),
            any(Pageable.class)
        );

        assertThat(descriptionCaptor.getValue()).isEqualTo("ACAO\\_100\\%");
    }

    @Test
    void shouldUseQueryWithoutDescriptionWhenFilterIsBlank() {
        when(jpaAssetRepository.searchByFiltersWithoutDescription(nullable(AssetType.class), any(Pageable.class)))
            .thenReturn(Page.empty());

        assetRepositoryAdapter.findPageOrdered(AssetType.STOCK, "   ", 0, 25);

        verify(jpaAssetRepository).searchByFiltersWithoutDescription(nullable(AssetType.class), any(Pageable.class));
        verify(jpaAssetRepository, never()).searchByFilters(
            nullable(AssetType.class),
            anyString(),
            any(Pageable.class)
        );
    }
}
