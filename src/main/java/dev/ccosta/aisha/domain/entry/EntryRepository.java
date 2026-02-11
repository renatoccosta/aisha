package dev.ccosta.aisha.domain.entry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EntryRepository {

    List<Entry> listTop100MostRecentBySettlementDate();

    Optional<Entry> findById(Long id);

    Entry save(Entry entry);

    boolean existsByCategoryId(Long categoryId);

    void deleteById(Long id);

    void deleteByIds(Collection<Long> ids);
}
