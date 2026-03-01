package dev.ccosta.aisha.domain.entry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import dev.ccosta.aisha.domain.shared.PagedResult;

public interface EntryRepository {

    PagedResult<Entry> listMostRecentBySettlementDateBetweenAndFilters(
        LocalDate startDate,
        LocalDate endDate,
        Long accountId,
        Long categoryId,
        boolean onlyWithoutCategory,
        boolean onlyPendingCategorySuggestions,
        int page,
        int pageSize
    );

    List<Entry> listAllBySettlementDateLessThanEqual(LocalDate endDate);

    List<EntryCategoryTrainingExample> listCategoryTrainingExamples();

    Optional<Entry> findById(Long id);

    Entry save(Entry entry);

    BigDecimal sumAmountByAccountIdAndSettlementDateBetween(Long accountId, LocalDate startDate, LocalDate endDate);

    Optional<LocalDate> findLatestSettlementDateByAccountId(Long accountId);

    boolean existsDuplicate(
        Long accountId,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        Long categoryId,
        BigDecimal amount,
        String externalId
    );

    boolean existsDuplicateIgnoringCategory(
        Long accountId,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        BigDecimal amount,
        String externalId
    );

    boolean existsByCategoryId(Long categoryId);

    boolean existsByAccountId(Long accountId);

    void deleteById(Long id);

    void deleteByIds(Collection<Long> ids);
}
