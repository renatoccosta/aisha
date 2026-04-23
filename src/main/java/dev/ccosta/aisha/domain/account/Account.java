package dev.ccosta.aisha.domain.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", length = 300)
    private String description;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private OpeningBalance openingBalance;

    @Column(name = "deactivation_date")
    private LocalDate deactivationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType = AccountType.OTHER;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getInitialBalance() {
        return openingBalance == null ? null : openingBalance.getAmount();
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        if (initialBalance == null && getInitialBalanceDate() == null) {
            setOpeningBalance(null);
            return;
        }

        OpeningBalance resolvedOpeningBalance = ensureOpeningBalance();
        resolvedOpeningBalance.setAmount(initialBalance);
    }

    public LocalDate getInitialBalanceDate() {
        return openingBalance == null ? null : openingBalance.getBalanceDate();
    }

    public void setInitialBalanceDate(LocalDate initialBalanceDate) {
        if (initialBalanceDate == null && getInitialBalance() == null) {
            setOpeningBalance(null);
            return;
        }

        OpeningBalance resolvedOpeningBalance = ensureOpeningBalance();
        resolvedOpeningBalance.setBalanceDate(initialBalanceDate);
    }

    public OpeningBalance getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(OpeningBalance openingBalance) {
        if (this.openingBalance != null) {
            this.openingBalance.setAccount(null);
        }

        this.openingBalance = openingBalance;
        if (openingBalance != null && openingBalance.getAccount() != this) {
            openingBalance.setAccount(this);
        }
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public LocalDate getDeactivationDate() {
        return deactivationDate;
    }

    public void setDeactivationDate(LocalDate deactivationDate) {
        this.deactivationDate = deactivationDate;
    }

    private OpeningBalance ensureOpeningBalance() {
        if (openingBalance == null) {
            setOpeningBalance(new OpeningBalance());
        }
        return openingBalance;
    }
}
