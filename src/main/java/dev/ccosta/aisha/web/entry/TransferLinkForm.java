package dev.ccosta.aisha.web.entry;

import jakarta.validation.constraints.NotNull;

public class TransferLinkForm {

    @NotNull(message = "{transferLinkForm.sourceEntryId.notNull}")
    private Long sourceEntryId;

    @NotNull(message = "{transferLinkForm.counterpartEntryId.notNull}")
    private Long counterpartEntryId;

    public Long getSourceEntryId() {
        return sourceEntryId;
    }

    public void setSourceEntryId(Long sourceEntryId) {
        this.sourceEntryId = sourceEntryId;
    }

    public Long getCounterpartEntryId() {
        return counterpartEntryId;
    }

    public void setCounterpartEntryId(Long counterpartEntryId) {
        this.counterpartEntryId = counterpartEntryId;
    }
}
