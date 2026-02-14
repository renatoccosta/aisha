package dev.ccosta.aisha.infrastructure.persistence.entry;

import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class EntryRepositoryAdapter implements EntryRepository {

    private final JpaEntryRepository jpaEntryRepository;

    public EntryRepositoryAdapter(JpaEntryRepository jpaEntryRepository) {
        this.jpaEntryRepository = jpaEntryRepository;
    }

    @Override
    public List<Entry> listTop100MostRecentBySettlementDate() {
        return jpaEntryRepository.findTop100ByOrderBySettlementDateDescIdDesc();
    }

    @Override
    public List<Entry> listTop100MostRecentBySettlementDateBetween(LocalDate startDate, LocalDate endDate) {
        return jpaEntryRepository.findTop100BySettlementDateBetweenOrderBySettlementDateDescIdDesc(startDate, endDate);
    }

    @Override
    public List<Entry> listTop100MostRecentBySettlementDateBetweenAndFilters(
        LocalDate startDate,
        LocalDate endDate,
        Long accountId,
        Long categoryId
    ) {
        if (accountId != null && categoryId != null) {
            return jpaEntryRepository.findTop100BySettlementDateBetweenAndAccountIdAndCategoryIdOrderBySettlementDateDescIdDesc(
                startDate,
                endDate,
                accountId,
                categoryId
            );
        }

        if (accountId != null) {
            return jpaEntryRepository.findTop100BySettlementDateBetweenAndAccountIdOrderBySettlementDateDescIdDesc(
                startDate,
                endDate,
                accountId
            );
        }

        if (categoryId != null) {
            return jpaEntryRepository.findTop100BySettlementDateBetweenAndCategoryIdOrderBySettlementDateDescIdDesc(
                startDate,
                endDate,
                categoryId
            );
        }

        return jpaEntryRepository.findTop100BySettlementDateBetweenOrderBySettlementDateDescIdDesc(startDate, endDate);
    }

    @Override
    public List<Entry> listAllBySettlementDateLessThanEqual(LocalDate endDate) {
        return jpaEntryRepository.findBySettlementDateLessThanEqualOrderBySettlementDateAscIdAsc(endDate);
    }

    @Override
    public Optional<Entry> findById(Long id) {
        return jpaEntryRepository.findById(id);
    }

    @Override
    public Entry save(Entry entry) {
        return jpaEntryRepository.save(entry);
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return jpaEntryRepository.existsByCategoryId(categoryId);
    }

    @Override
    public boolean existsByAccountId(Long accountId) {
        return jpaEntryRepository.existsByAccountId(accountId);
    }

    @Override
    public void deleteById(Long id) {
        jpaEntryRepository.deleteById(id);
    }

    @Override
    public void deleteByIds(Collection<Long> ids) {
        jpaEntryRepository.deleteAllByIdInBatch(ids);
    }
}
