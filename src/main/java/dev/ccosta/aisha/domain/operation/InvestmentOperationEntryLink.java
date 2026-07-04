package dev.ccosta.aisha.domain.operation;

import dev.ccosta.aisha.domain.entry.Entry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * Associates an investment operation with a financial entry and optionally stores the allocated amount.
 */
@Entity
@Table(
    name = "investment_operation_entries",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_investment_operation_entries_operation", columnNames = "operation_id"),
        @UniqueConstraint(name = "uk_investment_operation_entries_entry", columnNames = "entry_id")
    }
)
public class InvestmentOperationEntryLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id", nullable = false)
    private InvestmentOperation operation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private Entry entry;

    @Column(name = "allocated_amount", precision = 19, scale = 2)
    private BigDecimal allocatedAmount;

    public Long getId() {
        return id;
    }

    public InvestmentOperation getOperation() {
        return operation;
    }

    public void setOperation(InvestmentOperation operation) {
        this.operation = operation;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }
}
