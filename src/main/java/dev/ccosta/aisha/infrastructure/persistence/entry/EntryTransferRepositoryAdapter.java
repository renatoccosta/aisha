package dev.ccosta.aisha.infrastructure.persistence.entry;

import dev.ccosta.aisha.domain.entry.EntryTransfer;
import dev.ccosta.aisha.domain.entry.EntryTransferRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class EntryTransferRepositoryAdapter implements EntryTransferRepository {

    private final JpaEntryTransferRepository jpaEntryTransferRepository;

    public EntryTransferRepositoryAdapter(JpaEntryTransferRepository jpaEntryTransferRepository) {
        this.jpaEntryTransferRepository = jpaEntryTransferRepository;
    }

    @Override
    public EntryTransfer save(EntryTransfer entryTransfer) {
        return jpaEntryTransferRepository.save(entryTransfer);
    }

    @Override
    public Optional<EntryTransfer> findByEntryId(Long entryId) {
        return jpaEntryTransferRepository.findByEntryId(entryId);
    }

    @Override
    public boolean existsByEntryId(Long entryId) {
        return jpaEntryTransferRepository.existsByEntryId(entryId);
    }

    @Override
    public List<EntryTransfer> findAllByEntryIds(Collection<Long> entryIds) {
        return jpaEntryTransferRepository.findAllByEntryIds(entryIds);
    }

    @Override
    public void deleteAllByEntryIds(Collection<Long> entryIds) {
        jpaEntryTransferRepository.deleteAllByEntryIds(entryIds);
    }

    @Override
    public void delete(EntryTransfer entryTransfer) {
        jpaEntryTransferRepository.delete(entryTransfer);
    }
}
