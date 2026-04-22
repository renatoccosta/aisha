package dev.ccosta.aisha.domain.entry;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.categorization.EntryCategorySuggestionStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "entries")
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "category_id", nullable = true)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "suggested_category_id")
    private Category suggestedCategory;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_suggestion_status", nullable = false, length = 20)
    private EntryCategorySuggestionStatus categorySuggestionStatus = EntryCategorySuggestionStatus.NONE;

    @Column(name = "category_suggestion_confidence")
    private Double categorySuggestionConfidence;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_source", length = 20)
    private EntrySource entrySource;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private EntryType entryType = EntryType.REGULAR;

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Category getSuggestedCategory() {
        return suggestedCategory;
    }

    public void setSuggestedCategory(Category suggestedCategory) {
        this.suggestedCategory = suggestedCategory;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public EntryCategorySuggestionStatus getCategorySuggestionStatus() {
        return categorySuggestionStatus;
    }

    public void setCategorySuggestionStatus(EntryCategorySuggestionStatus categorySuggestionStatus) {
        this.categorySuggestionStatus = categorySuggestionStatus == null ? EntryCategorySuggestionStatus.NONE : categorySuggestionStatus;
    }

    public Double getCategorySuggestionConfidence() {
        return categorySuggestionConfidence;
    }

    public void setCategorySuggestionConfidence(Double categorySuggestionConfidence) {
        this.categorySuggestionConfidence = categorySuggestionConfidence;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public EntrySource getEntrySource() {
        return entrySource;
    }

    public void setEntrySource(EntrySource entrySource) {
        this.entrySource = entrySource;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntryType entryType) {
        this.entryType = entryType == null ? EntryType.REGULAR : entryType;
    }

    public boolean hasPendingCategorySuggestion() {
        return categorySuggestionStatus == EntryCategorySuggestionStatus.PENDING;
    }

    public boolean isTransfer() {
        return entryType == EntryType.TRANSFER;
    }
}
