package dev.ccosta.aisha.application.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.InvestmentOperation;
import dev.ccosta.aisha.domain.investment.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetPositionServiceTest {

    @Mock
    private AssetService assetService;

    @Mock
    private InvestmentOperationRepository investmentOperationRepository;

    @InjectMocks
    private AssetPositionService assetPositionService;

    @Test
    void shouldCalculateCurrentPositionAndSeparateCalculationMemory() {
        Asset asset = new Asset();
        asset.setAccount(new Account());
        asset.setName("PETR4");
        asset.setCurrency("BRL");
        asset.setOpeningPositionDate(LocalDate.of(2026, 1, 31));
        asset.setOpeningPositionQuantity(new BigDecimal("10.0000000000"));
        asset.setOpeningPositionTotalCost(new BigDecimal("1000.00"));
        asset.setOpeningPositionCurrency("BRL");

        InvestmentOperation buy = operation(
            InvestmentOperationType.BUY,
            LocalDate.of(2026, 2, 5),
            "5.0000000000",
            "600.00",
            null,
            "Compra"
        );
        InvestmentOperation dividend = operation(
            InvestmentOperationType.DIVIDEND,
            LocalDate.of(2026, 2, 10),
            null,
            "45.00",
            null,
            "Dividendo"
        );
        InvestmentOperation sell = operation(
            InvestmentOperationType.SELL,
            LocalDate.of(2026, 2, 20),
            "4.0000000000",
            "520.00",
            null,
            "Venda"
        );
        InvestmentOperation fee = operation(
            InvestmentOperationType.FEE,
            LocalDate.of(2026, 2, 22),
            null,
            "12.00",
            null,
            "Taxa"
        );
        InvestmentOperation amortization = operation(
            InvestmentOperationType.AMORTIZATION,
            LocalDate.of(2026, 2, 25),
            null,
            "100.00",
            null,
            "Amortização"
        );

        when(assetService.findById(1L)).thenReturn(asset);
        when(investmentOperationRepository.findAllByAssetIdOrdered(1L))
            .thenReturn(List.of(buy, dividend, sell, fee, amortization));

        AssetPositionDetails details = assetPositionService.buildDetails(1L);

        assertThat(details.currentPosition().quantity()).isEqualByComparingTo("11.0000000000");
        assertThat(details.currentPosition().totalCost()).isEqualByComparingTo("1073.33");
        assertThat(details.currentPosition().averageCost()).isEqualByComparingTo("97.57545455");
        assertThat(details.currentPosition().currency()).isEqualTo("BRL");
        assertThat(details.currentPosition().baseDate()).isEqualTo(LocalDate.of(2026, 2, 25));

        assertThat(details.quantityMovements()).hasSize(2);
        assertThat(details.quantityMovements().get(0).operationType()).isEqualTo(InvestmentOperationType.BUY);
        assertThat(details.quantityMovements().get(0).resultingQuantity()).isEqualByComparingTo("15.0000000000");
        assertThat(details.quantityMovements().get(0).resultingTotalCost()).isEqualByComparingTo("1600.00");
        assertThat(details.quantityMovements().get(1).operationType()).isEqualTo(InvestmentOperationType.SELL);
        assertThat(details.quantityMovements().get(1).quantityDelta()).isEqualByComparingTo("-4.0000000000");
        assertThat(details.quantityMovements().get(1).resultingQuantity()).isEqualByComparingTo("11.0000000000");
        assertThat(details.quantityMovements().get(1).resultingTotalCost()).isEqualByComparingTo("1173.33");

        assertThat(details.cashEvents()).hasSize(3);
        assertThat(details.cashEvents()).extracting(AssetCalculationEntry::operationType)
            .containsExactly(
                InvestmentOperationType.DIVIDEND,
                InvestmentOperationType.FEE,
                InvestmentOperationType.AMORTIZATION
            );
        assertThat(details.cashEvents().get(0).cashAmount()).isEqualByComparingTo("45.00");
        assertThat(details.cashEvents().get(1).cashAmount()).isEqualByComparingTo("-12.00");
        assertThat(details.cashEvents().get(2).costDelta()).isEqualByComparingTo("-100.00");
        assertThat(details.cashEvents().get(2).resultingTotalCost()).isEqualByComparingTo("1073.33");
    }

    private InvestmentOperation operation(
        InvestmentOperationType type,
        LocalDate tradeDate,
        String quantity,
        String netAmount,
        String unitPrice,
        String notes
    ) {
        InvestmentOperation operation = new InvestmentOperation();
        operation.setOperationType(type);
        operation.setTradeDate(tradeDate);
        operation.setQuantity(quantity == null ? null : new BigDecimal(quantity));
        operation.setNetAmount(netAmount == null ? null : new BigDecimal(netAmount));
        operation.setUnitPrice(unitPrice == null ? null : new BigDecimal(unitPrice));
        operation.setCurrency("BRL");
        operation.setNotes(notes);
        return operation;
    }
}
