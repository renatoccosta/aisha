package dev.ccosta.aisha.domain.account;

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
 * Represents the opening balance snapshot associated with an account.
 */
@Entity
@Table(name = "opening_balances")
public class OpeningBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_date")
    private LocalDate balanceDate;

    public Long getId() {
        return id;
    }

    /**
     * Returns the account that owns this opening balance snapshot.
     *
     * @return the associated account
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Associates this opening balance snapshot with an account.
     *
     * @param account the owning account
     */
    public void setAccount(Account account) {
        this.account = account;
    }

    /**
     * Returns the opening balance amount used as the account starting point.
     *
     * @return the opening balance amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Updates the opening balance amount used as the account starting point.
     *
     * @param amount the opening balance amount
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Returns the date represented by this opening balance snapshot.
     *
     * @return the opening balance date
     */
    public LocalDate getBalanceDate() {
        return balanceDate;
    }

    /**
     * Updates the date represented by this opening balance snapshot.
     *
     * @param balanceDate the opening balance date
     */
    public void setBalanceDate(LocalDate balanceDate) {
        this.balanceDate = balanceDate;
    }
}
