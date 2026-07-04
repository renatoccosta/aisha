package dev.ccosta.aisha.domain.operation;

import dev.ccosta.aisha.domain.asset.Asset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents the initial position snapshot for one investment asset.
 */
@Entity
@Table(name = "investment_asset_opening_positions")
public class OpeningPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false, unique = true)
    private Asset asset;

    @Column(name = "position_date", nullable = false)
    private LocalDate positionDate;

    @Column(name = "quantity", precision = 28, scale = 10, nullable = false)
    private BigDecimal quantity;

    @Column(name = "total_cost", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalCost;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    public Long getId() {
        return id;
    }

    /**
     * Returns the asset associated with this opening position snapshot.
     *
     * @return the related asset
     */
    public Asset getAsset() {
        return asset;
    }

    /**
     * Associates this opening position snapshot with one asset.
     *
     * @param asset the related asset
     */
    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    /**
     * Returns the snapshot reference date for the opening position.
     *
     * @return the opening position date
     */
    public LocalDate getPositionDate() {
        return positionDate;
    }

    /**
     * Updates the snapshot reference date for the opening position.
     *
     * @param positionDate the opening position date
     */
    public void setPositionDate(LocalDate positionDate) {
        this.positionDate = positionDate;
    }

    /**
     * Returns the quantity held at the opening snapshot date.
     *
     * @return the opening quantity
     */
    public BigDecimal getQuantity() {
        return quantity;
    }

    /**
     * Updates the quantity held at the opening snapshot date.
     *
     * @param quantity the opening quantity
     */
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    /**
     * Returns the total acquisition cost for the opening position.
     *
     * @return the opening total cost
     */
    public BigDecimal getTotalCost() {
        return totalCost;
    }

    /**
     * Updates the total acquisition cost for the opening position.
     *
     * @param totalCost the opening total cost
     */
    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    /**
     * Returns the ISO currency code used by the opening position snapshot.
     *
     * @return the opening position currency
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Updates the ISO currency code used by the opening position snapshot.
     *
     * @param currency the opening position currency
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
