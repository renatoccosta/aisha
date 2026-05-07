package dev.ccosta.aisha.web.entry;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccosta.aisha.application.entry.EntryRelationSummary;
import dev.ccosta.aisha.application.entry.transfer.EntryTransferView;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EntryActionStateTest {

    @Test
    void shouldAllowRegularActionsForEntryWithoutRelationships() {
        EntryActionState state = EntryActionState.from(entryWithType(10L, EntryType.REGULAR), EntryRelationSummary.empty(10L));

        assertThat(state.detailsAllowed()).isTrue();
        assertThat(state.regularEditAllowed()).isTrue();
        assertThat(state.createLinkedOperationAllowed()).isTrue();
        assertThat(state.createCounterpartAllowed()).isTrue();
        assertThat(state.linkTransferAllowed()).isTrue();
        assertThat(state.deleteAllowed()).isTrue();
        assertThat(state.transferEditAllowed()).isFalse();
        assertThat(state.unlinkTransferAllowed()).isFalse();
    }

    @Test
    void shouldAllowOnlyTransferActionsForTransferEntry() {
        EntryRelationSummary relationSummary = EntryRelationSummary.empty(10L)
            .withTransferView(new EntryTransferView(1L, 11L, 2L, "Reserva", true));

        EntryActionState state = EntryActionState.from(entryWithType(10L, EntryType.TRANSFER), relationSummary);

        assertThat(state.detailsAllowed()).isTrue();
        assertThat(state.transferEditAllowed()).isTrue();
        assertThat(state.unlinkTransferAllowed()).isTrue();
        assertThat(state.regularEditAllowed()).isFalse();
        assertThat(state.deleteAllowed()).isFalse();
    }

    @Test
    void shouldAllowOnlyDetailsForInvestmentLinkedEntry() {
        EntryActionState state = EntryActionState.from(
            entryWithType(10L, EntryType.REGULAR),
            EntryRelationSummary.empty(10L).withInvestmentOperationId(20L)
        );

        assertThat(state.detailsAllowed()).isTrue();
        assertThat(state.regularEditAllowed()).isFalse();
        assertThat(state.transferEditAllowed()).isFalse();
        assertThat(state.createLinkedOperationAllowed()).isFalse();
        assertThat(state.deleteAllowed()).isFalse();
    }

    private Entry entryWithType(Long id, EntryType entryType) {
        Entry entry = new Entry();
        ReflectionTestUtils.setField(entry, "id", id);
        entry.setEntryType(entryType);
        return entry;
    }
}
