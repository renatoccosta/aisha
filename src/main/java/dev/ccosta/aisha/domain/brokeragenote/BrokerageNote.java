package dev.ccosta.aisha.domain.brokeragenote;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents an imported brokerage note used to audit imported trades and avoid duplicate imports.
 */
@Entity
@Table(
    name = "brokerage_notes",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_brokerage_notes_broker_note_trade_date",
            columnNames = {"broker_cnpj", "note_number", "trade_date"}
        )
    }
)
public class BrokerageNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "broker_name", nullable = false, length = 200)
    private String brokerName;

    @Column(name = "broker_cnpj", nullable = false, length = 18)
    private String brokerCnpj;

    @Column(name = "note_number", nullable = false, length = 40)
    private String noteNumber;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "total_costs", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCosts;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_hash", nullable = false, length = 128)
    private String fileHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "net_entry_id", nullable = false)
    private Entry netEntry;

    public Long getId() {
        return id;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getBrokerCnpj() {
        return brokerCnpj;
    }

    public void setBrokerCnpj(String brokerCnpj) {
        this.brokerCnpj = brokerCnpj;
    }

    public String getNoteNumber() {
        return noteNumber;
    }

    public void setNoteNumber(String noteNumber) {
        this.noteNumber = noteNumber;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public BigDecimal getTotalCosts() {
        return totalCosts;
    }

    public void setTotalCosts(BigDecimal totalCosts) {
        this.totalCosts = totalCosts;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public Entry getNetEntry() {
        return netEntry;
    }

    public void setNetEntry(Entry netEntry) {
        this.netEntry = netEntry;
    }
}
