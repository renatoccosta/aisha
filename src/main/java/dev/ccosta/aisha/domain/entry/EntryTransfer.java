package dev.ccosta.aisha.domain.entry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Links the two entries that represent a transfer between accounts.
 */
@Entity
@Table(name = "entry_transfers")
public class EntryTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_entry_id", nullable = false, unique = true)
    private Entry originEntry;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_entry_id", nullable = false, unique = true)
    private Entry destinationEntry;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    public Long getId() {
        return id;
    }

    public Entry getOriginEntry() {
        return originEntry;
    }

    public void setOriginEntry(Entry originEntry) {
        this.originEntry = originEntry;
    }

    public Entry getDestinationEntry() {
        return destinationEntry;
    }

    public void setDestinationEntry(Entry destinationEntry) {
        this.destinationEntry = destinationEntry;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
