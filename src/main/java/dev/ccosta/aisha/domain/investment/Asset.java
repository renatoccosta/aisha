package dev.ccosta.aisha.domain.investment;

import dev.ccosta.aisha.domain.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

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

    @Column(name = "indexer_spread", length = 80)
    private String indexerSpread;

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
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

    public String getIndexerSpread() {
        return indexerSpread;
    }

    public void setIndexerSpread(String indexerSpread) {
        this.indexerSpread = indexerSpread;
    }
}
