package dev.ccosta.aisha.web.account;

import dev.ccosta.aisha.domain.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class AccountForm {

    @NotBlank(message = "{accountForm.title.notBlank}")
    @Size(max = 120, message = "{accountForm.title.size}")
    private String title;

    @Size(max = 300, message = "{accountForm.description.size}")
    private String description;

    @NotNull(message = "{accountForm.initialBalance.notNull}")
    @Digits(integer = 17, fraction = 2, message = "{accountForm.initialBalance.digits}")
    private BigDecimal initialBalance;

    @NotNull(message = "{accountForm.initialBalanceDate.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate initialBalanceDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deactivationDate;

    @NotNull(message = "{accountForm.accountType.notNull}")
    private AccountType accountType = AccountType.OTHER;

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
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

    public LocalDate getInitialBalanceDate() {
        return initialBalanceDate;
    }

    public void setInitialBalanceDate(LocalDate initialBalanceDate) {
        this.initialBalanceDate = initialBalanceDate;
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
}
