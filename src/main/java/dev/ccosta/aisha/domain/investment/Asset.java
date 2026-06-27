package dev.ccosta.aisha.domain.investment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a financial instrument held or tracked through an investment account.
 */
@Entity
@Table(name = "investment_assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 30)
    private AssetType type = AssetType.OTHER;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "ticker", length = 40)
    private String ticker;

    @Column(name = "isin", length = 20)
    private String isin;

    @Column(name = "issuer", length = 200)
    private String issuer;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BRL";

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "indexer_type", nullable = false, length = 20)
    private AssetIndexerType indexerType = AssetIndexerType.NONE;

    @Column(name = "indexer_spread", precision = 9, scale = 6)
    private BigDecimal indexerSpread;

    @OneToOne(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private OpeningPosition openingPosition;

    public Long getId() {
        return id;
    }

    public AssetType getType() {
        return type;
    }

    public void setType(AssetType type) {
        this.type = type == null ? AssetType.OTHER : type;
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
        this.indexerType = indexerType == null ? AssetIndexerType.NONE : indexerType;
    }

    public BigDecimal getIndexerSpread() {
        return indexerSpread;
    }

    public void setIndexerSpread(BigDecimal indexerSpread) {
        this.indexerSpread = indexerSpread;
    }

    public LocalDate getOpeningPositionDate() {
        return openingPosition == null ? null : openingPosition.getPositionDate();
    }

    public void setOpeningPositionDate(LocalDate openingPositionDate) {
        if (openingPositionDate == null
            && getOpeningPositionQuantity() == null
            && getOpeningPositionTotalCost() == null
            && getOpeningPositionCurrency() == null) {
            setOpeningPosition(null);
            return;
        }

        ensureOpeningPosition().setPositionDate(openingPositionDate);
    }

    public BigDecimal getOpeningPositionQuantity() {
        return openingPosition == null ? null : openingPosition.getQuantity();
    }

    public void setOpeningPositionQuantity(BigDecimal openingPositionQuantity) {
        if (openingPositionQuantity == null
            && getOpeningPositionDate() == null
            && getOpeningPositionTotalCost() == null
            && getOpeningPositionCurrency() == null) {
            setOpeningPosition(null);
            return;
        }

        ensureOpeningPosition().setQuantity(openingPositionQuantity);
    }

    public BigDecimal getOpeningPositionTotalCost() {
        return openingPosition == null ? null : openingPosition.getTotalCost();
    }

    public void setOpeningPositionTotalCost(BigDecimal openingPositionTotalCost) {
        if (openingPositionTotalCost == null
            && getOpeningPositionDate() == null
            && getOpeningPositionQuantity() == null
            && getOpeningPositionCurrency() == null) {
            setOpeningPosition(null);
            return;
        }

        ensureOpeningPosition().setTotalCost(openingPositionTotalCost);
    }

    public String getOpeningPositionCurrency() {
        return openingPosition == null ? null : openingPosition.getCurrency();
    }

    public void setOpeningPositionCurrency(String openingPositionCurrency) {
        if (openingPositionCurrency == null
            && getOpeningPositionDate() == null
            && getOpeningPositionQuantity() == null
            && getOpeningPositionTotalCost() == null) {
            setOpeningPosition(null);
            return;
        }

        ensureOpeningPosition().setCurrency(openingPositionCurrency);
    }

    public OpeningPosition getOpeningPosition() {
        return openingPosition;
    }

    public void setOpeningPosition(OpeningPosition openingPosition) {
        if (this.openingPosition != null) {
            this.openingPosition.setAsset(null);
        }

        this.openingPosition = openingPosition;
        if (openingPosition != null && openingPosition.getAsset() != this) {
            openingPosition.setAsset(this);
        }
    }

    private OpeningPosition ensureOpeningPosition() {
        if (openingPosition == null) {
            setOpeningPosition(new OpeningPosition());
        }
        return openingPosition;
    }
}
