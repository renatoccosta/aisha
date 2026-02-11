package dev.ccosta.aisha.web.entry;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class EntryForm {

    @NotNull(message = "{entryForm.accountId.notNull}")
    private Long accountId;

    @NotNull(message = "{entryForm.movementDate.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate movementDate;

    @NotNull(message = "{entryForm.settlementDate.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate settlementDate;

    @NotBlank(message = "{entryForm.description.notBlank}")
    @Size(max = 200, message = "{entryForm.description.size}")
    private String description;

    private Long categoryId;

    @Size(max = 120, message = "{entryForm.newCategoryTitle.size}")
    private String newCategoryTitle;

    @Size(max = 1000, message = "{entryForm.notes.size}")
    private String notes;

    @NotNull(message = "{entryForm.amount.notNull}")
    @Digits(integer = 17, fraction = 2, message = "{entryForm.amount.digits}")
    private BigDecimal amount;

    public static EntryForm newWithCurrentDates() {
        EntryForm form = new EntryForm();
        LocalDate today = LocalDate.now();
        form.setMovementDate(today);
        form.setSettlementDate(today);
        return form;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public LocalDate getMovementDate() {
        return movementDate;
    }

    public void setMovementDate(LocalDate movementDate) {
        this.movementDate = movementDate;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getNewCategoryTitle() {
        return newCategoryTitle;
    }

    public void setNewCategoryTitle(String newCategoryTitle) {
        this.newCategoryTitle = newCategoryTitle;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
