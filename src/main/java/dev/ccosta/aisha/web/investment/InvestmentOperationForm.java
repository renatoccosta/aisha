package dev.ccosta.aisha.web.investment;

import dev.ccosta.aisha.domain.investment.InvestmentOperationSourceType;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Captures user input for creating and editing investment operations.
 */
public class InvestmentOperationForm {

    @NotNull(message = "{investmentOperationForm.assetId.notNull}")
    private Long assetId;

    @Size(max = 200, message = "{investmentOperationForm.newAssetName.size}")
    private String newAssetName;

    @NotNull(message = "{investmentOperationForm.accountId.notNull}")
    private Long accountId;

    @NotNull(message = "{investmentOperationForm.operationType.notNull}")
    private InvestmentOperationType operationType = InvestmentOperationType.BUY;

    @NotNull(message = "{investmentOperationForm.tradeDate.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate tradeDate = LocalDate.now();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate settlementDate;

    @Digits(integer = 18, fraction = 10, message = "{investmentOperationForm.quantity.digits}")
    private BigDecimal quantity;

    @Digits(integer = 11, fraction = 8, message = "{investmentOperationForm.unitPrice.digits}")
    private BigDecimal unitPrice;

    @Digits(integer = 17, fraction = 2, message = "{investmentOperationForm.money.digits}")
    private BigDecimal grossAmount;

    @Digits(integer = 17, fraction = 2, message = "{investmentOperationForm.money.digits}")
    private BigDecimal netAmount;

    @Digits(integer = 17, fraction = 2, message = "{investmentOperationForm.money.digits}")
    private BigDecimal fees;

    @Digits(integer = 17, fraction = 2, message = "{investmentOperationForm.money.digits}")
    private BigDecimal taxes;

    @NotBlank(message = "{investmentOperationForm.currency.notBlank}")
    @Pattern(regexp = "[A-Za-z]{3}", message = "{investmentOperationForm.currency.pattern}")
    private String currency = "BRL";

    @Size(max = 1000, message = "{investmentOperationForm.notes.size}")
    private String notes;

    @Digits(integer = 3, fraction = 6, message = "{investmentOperationForm.indexerSpread.digits}")
    private BigDecimal indexerSpread;

    @NotNull(message = "{investmentOperationForm.sourceType.notNull}")
    private InvestmentOperationSourceType sourceType = InvestmentOperationSourceType.MANUAL;

    private Long brokerageNoteId;

    private List<Long> linkedEntryIds = new ArrayList<>();

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getNewAssetName() {
        return newAssetName;
    }

    public void setNewAssetName(String newAssetName) {
        this.newAssetName = newAssetName;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public InvestmentOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(InvestmentOperationType operationType) {
        this.operationType = operationType;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public BigDecimal getFees() {
        return fees;
    }

    public void setFees(BigDecimal fees) {
        this.fees = fees;
    }

    public BigDecimal getTaxes() {
        return taxes;
    }

    public void setTaxes(BigDecimal taxes) {
        this.taxes = taxes;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getIndexerSpread() {
        return indexerSpread;
    }

    public void setIndexerSpread(BigDecimal indexerSpread) {
        this.indexerSpread = indexerSpread;
    }

    public InvestmentOperationSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(InvestmentOperationSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public Long getBrokerageNoteId() {
        return brokerageNoteId;
    }

    public void setBrokerageNoteId(Long brokerageNoteId) {
        this.brokerageNoteId = brokerageNoteId;
    }

    public List<Long> getLinkedEntryIds() {
        return linkedEntryIds;
    }

    public void setLinkedEntryIds(List<Long> linkedEntryIds) {
        this.linkedEntryIds = linkedEntryIds == null ? new ArrayList<>() : linkedEntryIds;
    }
}
