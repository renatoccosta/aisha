package dev.ccosta.aisha.web.entry;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class EntryForm {

    @NotBlank(message = "Conta é obrigatória")
    @Size(max = 120, message = "Conta deve ter no máximo 120 caracteres")
    private String account;

    @NotNull(message = "Data de movimentação é obrigatória")
    private LocalDate movementDate;

    @NotNull(message = "Data de liquidação é obrigatória")
    private LocalDate settlementDate;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 200, message = "Descrição deve ter no máximo 200 caracteres")
    private String description;

    @NotBlank(message = "Categoria é obrigatória")
    @Size(max = 60, message = "Categoria deve ter no máximo 60 caracteres")
    private String category;

    @Size(max = 1000, message = "Observações deve ter no máximo 1000 caracteres")
    private String notes;

    @NotNull(message = "Valor é obrigatório")
    @Digits(integer = 17, fraction = 2, message = "Valor deve ter até 2 casas decimais")
    private BigDecimal amount;

    public static EntryForm newWithCurrentDates() {
        EntryForm form = new EntryForm();
        LocalDate today = LocalDate.now();
        form.setMovementDate(today);
        form.setSettlementDate(today);
        return form;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
