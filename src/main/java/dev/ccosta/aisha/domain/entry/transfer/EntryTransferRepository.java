package dev.ccosta.aisha.domain.entry.transfer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EntryTransferRepository {

    EntryTransfer save(EntryTransfer entryTransfer);

    Optional<EntryTransfer> findByEntryId(Long entryId);

    boolean existsByEntryId(Long entryId);

    List<EntryTransfer> findAllByEntryIds(Collection<Long> entryIds);

    void deleteAllByEntryIds(Collection<Long> entryIds);

    void delete(EntryTransfer entryTransfer);
}
