package dev.ccosta.aisha.web.operation;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.asset.Asset;
import dev.ccosta.aisha.domain.operation.InvestmentOperationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EntryLinkedOperationPrefillBuilderTest {

    private final EntryLinkedOperationPrefillBuilder builder = new EntryLinkedOperationPrefillBuilder();

    @Test
    void shouldPrefillOperationFormFromEntryAndExistingAsset() {
        Entry entry = entry("Compra PETR4 quantidade 10", new BigDecimal("-125.50"));
        Asset asset = asset(10L, "Petrobras PN", "PETR4");

        InvestmentOperationForm form = builder.build(entry, List.of(asset), -1L);

        assertThat(form.getAccountId()).isEqualTo(20L);
        assertThat(form.getAssetId()).isEqualTo(10L);
        assertThat(form.getOperationType()).isEqualTo(InvestmentOperationType.BUY);
        assertThat(form.getTradeDate()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(form.getSettlementDate()).isEqualTo(LocalDate.of(2026, 4, 22));
        assertThat(form.getQuantity()).isEqualByComparingTo("10");
        assertThat(form.getGrossAmount()).isEqualByComparingTo("-125.50");
        assertThat(form.getNetAmount()).isEqualByComparingTo("-125.50");
        assertThat(form.getFees()).isEqualByComparingTo("0");
        assertThat(form.getTaxes()).isEqualByComparingTo("0");
        assertThat(form.getNotes()).isEqualTo("Compra PETR4 quantidade 10");
        assertThat(form.getLinkedEntryIds()).containsExactly(30L);
    }

    @Test
    void shouldSelectNewAssetWhenTickerIsClearAndNoSimilarAssetExists() {
        Entry entry = entry("Compra XPTO11 qtde 4", new BigDecimal("-400.00"));

        InvestmentOperationForm form = builder.build(entry, List.of(), -1L);

        assertThat(form.getAssetId()).isEqualTo(-1L);
        assertThat(form.getNewAssetName()).isEqualTo("XPTO11");
        assertThat(form.getQuantity()).isEqualByComparingTo("4");
    }

    @Test
    void shouldLeaveAssetEmptyWhenDescriptionHasNoClearAssetName() {
        Entry entry = entry("Aplicação automática investimento", new BigDecimal("-100.00"));

        InvestmentOperationForm form = builder.build(entry, List.of(), -1L);

        assertThat(form.getAssetId()).isNull();
        assertThat(form.getNewAssetName()).isNull();
        assertThat(form.getOperationType()).isEqualTo(InvestmentOperationType.BUY);
    }

    @Test
    void shouldInferIncomeAndRedemptionOperationTypes() {
        assertThat(builder.inferOperationType("JCP PETR4")).isEqualTo(InvestmentOperationType.INTEREST);
        assertThat(builder.inferOperationType("Dividendos MXRF11")).isEqualTo(InvestmentOperationType.DIVIDEND);
        assertThat(builder.inferOperationType("Resgate CDB Banco")).isEqualTo(InvestmentOperationType.REDEMPTION);
    }

    @Test
    void shouldInferInterestOnEquityEntryWithBrazilianThousandsQuantityAndTicker() {
        Entry entry = entry("JUROS S/ CAPITAL DE 2.200 DE BBAS3", new BigDecimal("250.00"));
        Asset asset = asset(11L, "Banco do Brasil ON", "BBAS3");

        InvestmentOperationForm form = builder.build(entry, List.of(asset), -1L);

        assertThat(form.getOperationType()).isEqualTo(InvestmentOperationType.INTEREST);
        assertThat(form.getQuantity()).isEqualByComparingTo("2200");
        assertThat(form.getAssetId()).isEqualTo(11L);
    }

    @Test
    void shouldInferRealIncomeEntryWithQuantityBeforeTicker() {
        Entry entry = entry("RENDIMENTOS 200 DE XPML11", new BigDecimal("180.00"));
        Asset asset = asset(12L, "XP Malls", "XPML11");

        InvestmentOperationForm form = builder.build(entry, List.of(asset), -1L);

        assertThat(form.getOperationType()).isEqualTo(InvestmentOperationType.INTEREST);
        assertThat(form.getQuantity()).isEqualByComparingTo("200");
        assertThat(form.getAssetId()).isEqualTo(12L);
    }

    @Test
    void shouldInferDividendEntryWithBrazilianThousandsQuantityAndTicker() {
        Entry entry = entry("DIVIDENDOS 2.700 DE PETR4", new BigDecimal("900.00"));
        Asset asset = asset(13L, "Petrobras PN", "PETR4");

        InvestmentOperationForm form = builder.build(entry, List.of(asset), -1L);

        assertThat(form.getOperationType()).isEqualTo(InvestmentOperationType.DIVIDEND);
        assertThat(form.getQuantity()).isEqualByComparingTo("2700");
        assertThat(form.getAssetId()).isEqualTo(13L);
    }

    @Test
    void shouldInferDividendQuantityAfterSlashUsingCommaAsThousandsSeparator() {
        Entry entry = entry("DIVIDENDOS DE CLIENTES CSMG3 S/          1,000", new BigDecimal("120.00"));
        Asset asset = asset(14L, "Copasa", "CSMG3");

        InvestmentOperationForm form = builder.build(entry, List.of(asset), -1L);

        assertThat(form.getOperationType()).isEqualTo(InvestmentOperationType.DIVIDEND);
        assertThat(form.getQuantity()).isEqualByComparingTo("1000");
        assertThat(form.getAssetId()).isEqualTo(14L);
    }

    @Test
    void shouldInferDescriptiveFixedIncomeAssetFromPurchaseEntry() {
        Entry entry = entry("COMPRA 10 CDB BANCOS - 1224127", new BigDecimal("-1000.00"));

        InvestmentOperationForm form = builder.build(entry, List.of(), -1L);

        assertThat(form.getOperationType()).isEqualTo(InvestmentOperationType.BUY);
        assertThat(form.getQuantity()).isEqualByComparingTo("10");
        assertThat(form.getAssetId()).isEqualTo(-1L);
        assertThat(form.getNewAssetName()).isEqualTo("CDB BANCOS - 1224127");
    }

    @Test
    void shouldKeepGenericPublicBondPurchaseWithoutAssetOrQuantity() {
        Entry entry = entry("COMPRA TIT PUBLICOS TD 18/11/20", new BigDecimal("-500.00"));

        InvestmentOperationForm form = builder.build(entry, List.of(), -1L);

        assertThat(form.getOperationType()).isEqualTo(InvestmentOperationType.BUY);
        assertThat(form.getQuantity()).isNull();
        assertThat(form.getAssetId()).isNull();
        assertThat(form.getNewAssetName()).isNull();
    }

    @Test
    void shouldKeepGenericPublicBondRedemptionWithoutAssetOrQuantity() {
        Entry entry = entry("RESGATE TITLS.PUBLICOS 17/08/20", new BigDecimal("500.00"));

        InvestmentOperationForm form = builder.build(entry, List.of(), -1L);

        assertThat(form.getOperationType()).isEqualTo(InvestmentOperationType.REDEMPTION);
        assertThat(form.getQuantity()).isNull();
        assertThat(form.getAssetId()).isNull();
        assertThat(form.getNewAssetName()).isNull();
    }

    @Test
    void shouldInferPublicBondTaxWithoutAssetOrQuantity() {
        Entry entry = entry("IR S/ TÍTULOS PÚBLICOS 17/08/20", new BigDecimal("-25.00"));

        InvestmentOperationForm form = builder.build(entry, List.of(), -1L);

        assertThat(form.getOperationType()).isEqualTo(InvestmentOperationType.TAX);
        assertThat(form.getQuantity()).isNull();
        assertThat(form.getAssetId()).isNull();
        assertThat(form.getNewAssetName()).isNull();
    }

    private Entry entry(String description, BigDecimal amount) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", 20L);
        account.setTitle("Investimentos");

        Entry entry = new Entry();
        ReflectionTestUtils.setField(entry, "id", 30L);
        entry.setAccount(account);
        entry.setDescription(description);
        entry.setMovementDate(LocalDate.of(2026, 4, 20));
        entry.setSettlementDate(LocalDate.of(2026, 4, 22));
        entry.setAmount(amount);
        return entry;
    }

    private Asset asset(Long id, String name, String ticker) {
        Asset asset = new Asset();
        ReflectionTestUtils.setField(asset, "id", id);
        asset.setName(name);
        asset.setTicker(ticker);
        return asset;
    }
}
