package dev.ccosta.aisha.web.entry;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class TransferForm {

    @NotNull(message = "{transferForm.originAccountId.notNull}")
    private Long originAccountId;

    @NotNull(message = "{transferForm.destinationAccountId.notNull}")
    private Long destinationAccountId;

    @NotNull(message = "{transferForm.movementDate.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate movementDate;

    @NotNull(message = "{transferForm.settlementDate.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate settlementDate;

    @NotBlank(message = "{transferForm.description.notBlank}")
    @Size(max = 200, message = "{transferForm.description.size}")
    private String description;

    @Size(max = 1000, message = "{transferForm.notes.size}")
    private String notes;

    @NotNull(message = "{transferForm.amount.notNull}")
    @Digits(integer = 17, fraction = 2, message = "{transferForm.amount.digits}")
    private BigDecimal amount;

    public static TransferForm newWithCurrentDates() {
        TransferForm form = new TransferForm();
        LocalDate today = LocalDate.now();
        form.setMovementDate(today);
        form.setSettlementDate(today);
        return form;
    }

    public Long getOriginAccountId() {
        return originAccountId;
    }

    public void setOriginAccountId(Long originAccountId) {
        this.originAccountId = originAccountId;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
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
