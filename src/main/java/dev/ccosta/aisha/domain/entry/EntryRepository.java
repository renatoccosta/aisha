package dev.ccosta.aisha.domain.entry;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EntryRepository {

    List<Entry> listTop100MostRecentBySettlementDate();

    List<Entry> listTop100MostRecentBySettlementDateBetween(LocalDate startDate, LocalDate endDate);

    List<Entry> listTop100MostRecentBySettlementDateBetweenAndFilters(LocalDate startDate, LocalDate endDate, Long accountId, Long categoryId);

    List<Entry> listAllBySettlementDateLessThanEqual(LocalDate endDate);

    Optional<Entry> findById(Long id);

    Entry save(Entry entry);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByAccountId(Long accountId);

    void deleteById(Long id);

    void deleteByIds(Collection<Long> ids);
}
