package dev.ccosta.aisha.infrastructure.persistence.asset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.asset.AssetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AssetRepositoryAdapterTest {

    @Mock
    private JpaAssetRepository jpaAssetRepository;

    @InjectMocks
    private AssetRepositoryAdapter assetRepositoryAdapter;

    @Test
    @SuppressWarnings("unchecked")
    void shouldQueryUsingSpecification() {
        when(jpaAssetRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        assetRepositoryAdapter.findPageOrdered(AssetType.STOCK, "Ação_100%", 0, 25);

        verify(jpaAssetRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldQueryUsingSpecificationWhenFilterIsBlank() {
        when(jpaAssetRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        assetRepositoryAdapter.findPageOrdered(AssetType.STOCK, "   ", 0, 25);

        verify(jpaAssetRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}
