package dev.ccosta.aisha.application.entry.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.entry.transfer.EntryTransfer;
import dev.ccosta.aisha.domain.entry.transfer.EntryTransferRepository;
import dev.ccosta.aisha.domain.entry.EntryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntryTransferServiceTest {

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private EntryTransferRepository entryTransferRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private EntryTransferService entryTransferService;

    @Test
    void shouldCreateTransferWithPairedEntries() {
        Account originAccount = newAccount(1L, "Conta origem");
        Account destinationAccount = newAccount(2L, "Conta destino");
        Entry savedOrigin = newEntry(10L, originAccount, "-150.00");
        Entry savedDestination = newEntry(11L, destinationAccount, "150.00");
        EntryTransfer savedTransfer = new EntryTransfer();
        savedTransfer.setOriginEntry(savedOrigin);
        savedTransfer.setDestinationEntry(savedDestination);

        when(accountService.findById(1L)).thenReturn(originAccount);
        when(accountService.findById(2L)).thenReturn(destinationAccount);
        when(entryRepository.save(any(Entry.class))).thenReturn(savedOrigin, savedDestination);
        when(entryTransferRepository.save(any(EntryTransfer.class))).thenReturn(savedTransfer);

        EntryTransfer transfer = entryTransferService.createTransfer(new EntryTransferCreationRequest(
            1L,
            2L,
            LocalDate.of(2026, 4, 10),
            LocalDate.of(2026, 4, 10),
            "Transferência",
            new BigDecimal("150.00"),
            "Reserva"
        ));

        assertThat(transfer.getOriginEntry().getAmount()).isEqualByComparingTo("-150.00");
        assertThat(transfer.getDestinationEntry().getAmount()).isEqualByComparingTo("150.00");

        ArgumentCaptor<Entry> entryCaptor = ArgumentCaptor.forClass(Entry.class);
        verify(entryRepository, times(2)).save(entryCaptor.capture());
        assertThat(entryCaptor.getAllValues())
            .allSatisfy(entry -> {
                assertThat(entry.getEntryType()).isEqualTo(EntryType.TRANSFER);
                assertThat(entry.getCategory()).isNull();
            });
    }

    @Test
    void shouldLinkExistingRegularEntriesAsTransfer() {
        Entry debitEntry = newEntry(1L, newAccount(1L, "Conta A"), "-50.00");
        debitEntry.setMovementDate(LocalDate.of(2026, 4, 10));
        debitEntry.setSettlementDate(LocalDate.of(2026, 4, 10));
        Entry creditEntry = newEntry(2L, newAccount(2L, "Conta B"), "50.00");
        creditEntry.setMovementDate(LocalDate.of(2026, 4, 10));
        creditEntry.setSettlementDate(LocalDate.of(2026, 4, 10));

        when(entryRepository.findById(1L)).thenReturn(Optional.of(debitEntry));
        when(entryRepository.findById(2L)).thenReturn(Optional.of(creditEntry));
        when(entryTransferRepository.existsByEntryId(1L)).thenReturn(false);
        when(entryTransferRepository.existsByEntryId(2L)).thenReturn(false);
        when(entryTransferRepository.save(any(EntryTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EntryTransfer transfer = entryTransferService.linkExistingEntries(1L, 2L);

        assertThat(transfer.getOriginEntry()).isEqualTo(debitEntry);
        assertThat(transfer.getDestinationEntry()).isEqualTo(creditEntry);
        assertThat(debitEntry.getEntryType()).isEqualTo(EntryType.TRANSFER);
        assertThat(creditEntry.getEntryType()).isEqualTo(EntryType.TRANSFER);
    }

    @Test
    void shouldCreateCounterpartFromExistingEntry() {
        Account sourceAccount = newAccount(1L, "Conta origem");
        Account counterpartAccount = newAccount(2L, "Conta destino");
        Entry sourceEntry = newEntry(1L, sourceAccount, "-80.00");
        sourceEntry.setMovementDate(LocalDate.of(2026, 4, 10));
        sourceEntry.setSettlementDate(LocalDate.of(2026, 4, 10));

        when(entryRepository.findById(1L)).thenReturn(Optional.of(sourceEntry));
        when(entryTransferRepository.existsByEntryId(1L)).thenReturn(false);
        when(accountService.findById(2L)).thenReturn(counterpartAccount);
        when(entryRepository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(entryTransferRepository.save(any(EntryTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EntryTransfer transfer = entryTransferService.createCounterpartFromEntry(1L, new EntryTransferCounterpartRequest(
            2L,
            LocalDate.of(2026, 4, 11),
            LocalDate.of(2026, 4, 11),
            "Transferência para poupança",
            "Reserva"
        ));

        assertThat(sourceEntry.getEntryType()).isEqualTo(EntryType.TRANSFER);
        assertThat(sourceEntry.getSettlementDate()).isEqualTo(LocalDate.of(2026, 4, 11));
        assertThat(transfer.getDestinationEntry().getAccount().getId()).isEqualTo(2L);
        assertThat(transfer.getDestinationEntry().getAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void shouldUnlinkTransferBackToRegularEntries() {
        Entry originEntry = newEntry(1L, newAccount(1L, "Conta A"), "-30.00");
        originEntry.setEntryType(EntryType.TRANSFER);
        Entry destinationEntry = newEntry(2L, newAccount(2L, "Conta B"), "30.00");
        destinationEntry.setEntryType(EntryType.TRANSFER);
        EntryTransfer transfer = new EntryTransfer();
        transfer.setOriginEntry(originEntry);
        transfer.setDestinationEntry(destinationEntry);

        when(entryTransferRepository.findByEntryId(1L)).thenReturn(Optional.of(transfer));
        when(entryRepository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        entryTransferService.unlinkTransferByEntryId(1L);

        verify(entryTransferRepository).delete(transfer);
        assertThat(originEntry.getEntryType()).isEqualTo(EntryType.REGULAR);
        assertThat(destinationEntry.getEntryType()).isEqualTo(EntryType.REGULAR);
    }

    @Test
    void shouldRejectLinkWhenAmountsDoNotMatch() {
        Entry debitEntry = newEntry(1L, newAccount(1L, "Conta A"), "-50.00");
        debitEntry.setMovementDate(LocalDate.of(2026, 4, 10));
        debitEntry.setSettlementDate(LocalDate.of(2026, 4, 10));
        Entry creditEntry = newEntry(2L, newAccount(2L, "Conta B"), "60.00");
        creditEntry.setMovementDate(LocalDate.of(2026, 4, 10));
        creditEntry.setSettlementDate(LocalDate.of(2026, 4, 10));

        when(entryRepository.findById(1L)).thenReturn(Optional.of(debitEntry));
        when(entryRepository.findById(2L)).thenReturn(Optional.of(creditEntry));
        when(entryTransferRepository.existsByEntryId(1L)).thenReturn(false);
        when(entryTransferRepository.existsByEntryId(2L)).thenReturn(false);

        assertThatThrownBy(() -> entryTransferService.linkExistingEntries(1L, 2L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("same absolute amount");
    }

    private Account newAccount(Long id, String title) {
        Account account = new Account();
        account.setTitle(title);
        setField(account, "id", id);
        return account;
    }

    private Entry newEntry(Long id, Account account, String amount) {
        Entry entry = new Entry();
        entry.setAccount(account);
        entry.setDescription("Transferência");
        entry.setAmount(new BigDecimal(amount));
        entry.setMovementDate(LocalDate.of(2026, 4, 10));
        entry.setSettlementDate(LocalDate.of(2026, 4, 10));
        setField(entry, "id", id);
        return entry;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
