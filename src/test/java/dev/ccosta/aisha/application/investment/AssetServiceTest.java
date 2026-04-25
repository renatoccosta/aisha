package dev.ccosta.aisha.application.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetIndexerType;
import dev.ccosta.aisha.domain.investment.AssetRepository;
import dev.ccosta.aisha.domain.investment.AssetType;
import dev.ccosta.aisha.domain.investment.InvestmentOperationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private InvestmentOperationRepository investmentOperationRepository;

    @InjectMocks
    private AssetService assetService;

    @Test
    void shouldCreateAssetWithDefaults() {
        Asset asset = new Asset();
        asset.setName("PETR4");
        asset.setTicker("PETR4");
        asset.setType(AssetType.STOCK);
        asset.setCurrency("brl");

        when(assetRepository.save(asset)).thenReturn(asset);

        Asset created = assetService.create(asset);

        assertThat(created.getCurrency()).isEqualTo("BRL");
        assertThat(created.getIndexerType()).isEqualTo(AssetIndexerType.NONE);
        verify(assetRepository).save(asset);
    }

    @Test
    void shouldCreateAssetWithOpeningPositionDefaults() {
        Asset asset = new Asset();
        asset.setName("Tesouro Selic");
        asset.setCurrency("brl");
        asset.setOpeningPositionDate(LocalDate.of(2026, 1, 31));
        asset.setOpeningPositionQuantity(new BigDecimal("15.5000000000"));
        asset.setOpeningPositionTotalCost(new BigDecimal("1500.25"));

        when(assetRepository.save(asset)).thenReturn(asset);

        Asset created = assetService.create(asset);

        assertThat(created.getOpeningPositionDate()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(created.getOpeningPositionQuantity()).isEqualByComparingTo("15.5000000000");
        assertThat(created.getOpeningPositionTotalCost()).isEqualByComparingTo("1500.25");
        assertThat(created.getOpeningPositionCurrency()).isEqualTo("BRL");
    }

    @Test
    void shouldRejectBlankAssetName() {
        Asset asset = new Asset();
        asset.setName(" ");

        assertThatThrownBy(() -> assetService.create(asset))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("name");

        verify(assetRepository, never()).save(asset);
    }

    @Test
    void shouldRejectPartialOpeningPosition() {
        Asset asset = new Asset();
        asset.setName("Tesouro Selic");
        asset.setOpeningPositionDate(LocalDate.of(2026, 1, 31));

        assertThatThrownBy(() -> assetService.create(asset))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Opening position");

        verify(assetRepository, never()).save(asset);
    }

    @Test
    void shouldPreventDeletingAssetInUse() {
        Asset asset = new Asset();

        when(assetRepository.findById(20L)).thenReturn(Optional.of(asset));
        when(investmentOperationRepository.existsByAssetId(20L)).thenReturn(true);

        assertThatThrownBy(() -> assetService.deleteById(20L))
            .isInstanceOf(AssetInUseException.class);

        verify(assetRepository, never()).deleteById(20L);
    }
}
