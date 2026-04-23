package dev.ccosta.aisha.web.investment;

import dev.ccosta.aisha.domain.investment.AssetIndexerType;
import dev.ccosta.aisha.domain.investment.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Captures user input for creating and editing investment assets.
 */
public class AssetForm {

    @NotNull(message = "{assetForm.accountId.notNull}")
    private Long accountId;

    @NotNull(message = "{assetForm.type.notNull}")
    private AssetType type = AssetType.OTHER;

    @NotBlank(message = "{assetForm.name.notBlank}")
    @Size(max = 200, message = "{assetForm.name.size}")
    private String name;

    @Size(max = 40, message = "{assetForm.ticker.size}")
    private String ticker;

    @Size(max = 20, message = "{assetForm.isin.size}")
    private String isin;

    @Size(max = 200, message = "{assetForm.issuer.size}")
    private String issuer;

    @NotBlank(message = "{assetForm.currency.notBlank}")
    @Pattern(regexp = "[A-Za-z]{3}", message = "{assetForm.currency.pattern}")
    private String currency = "BRL";

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate maturityDate;

    @NotNull(message = "{assetForm.indexerType.notNull}")
    private AssetIndexerType indexerType = AssetIndexerType.NONE;

    @Size(max = 80, message = "{assetForm.indexerSpread.size}")
    private String indexerSpread;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public AssetType getType() {
        return type;
    }

    public void setType(AssetType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }

    public AssetIndexerType getIndexerType() {
        return indexerType;
    }

    public void setIndexerType(AssetIndexerType indexerType) {
        this.indexerType = indexerType;
    }

    public String getIndexerSpread() {
        return indexerSpread;
    }

    public void setIndexerSpread(String indexerSpread) {
        this.indexerSpread = indexerSpread;
    }
}
