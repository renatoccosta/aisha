package dev.ccosta.aisha.infrastructure.persistence.entry;

import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryCategorySuggestionStatus;
import dev.ccosta.aisha.domain.entry.EntryCategoryTrainingExample;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class EntryRepositoryAdapter implements EntryRepository {

    private static final String ACCENTED_CHARACTERS = "ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑáàâãäéèêëíìîïóòôõöúùûüçñ";
    private static final String PLAIN_CHARACTERS = "AAAAAEEEEIIIIOOOOOUUUUCNaaaaaeeeeiiiiooooouuuucn";

    private final JpaEntryRepository jpaEntryRepository;

    public EntryRepositoryAdapter(JpaEntryRepository jpaEntryRepository) {
        this.jpaEntryRepository = jpaEntryRepository;
    }

    @Override
    public PagedResult<Entry> listMostRecentBySettlementDateBetweenAndFilters(
        LocalDate startDate,
        LocalDate endDate,
        Long accountId,
        Long categoryId,
        String descriptionFilter,
        boolean onlyWithoutCategory,
        boolean onlyPendingCategorySuggestions,
        int page,
        int pageSize
    ) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        Page<Entry> result = jpaEntryRepository.searchBySettlementDateBetweenAndFilters(
            startDate,
            endDate,
            accountId,
            categoryId,
            normalizeDescriptionFilter(descriptionFilter),
            onlyWithoutCategory,
            onlyPendingCategorySuggestions,
            EntryCategorySuggestionStatus.PENDING,
            ACCENTED_CHARACTERS,
            PLAIN_CHARACTERS,
            pageRequest
        );
        return new PagedResult<>(
            result.getContent(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public List<Entry> listAllBySettlementDateLessThanEqual(LocalDate endDate) {
        return jpaEntryRepository.findBySettlementDateLessThanEqualOrderBySettlementDateAscIdAsc(endDate);
    }

    @Override
    public List<EntryCategoryTrainingExample> listCategoryTrainingExamples() {
        return jpaEntryRepository.findCategoryTrainingExamples();
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
    public BigDecimal sumAmountByAccountIdAndSettlementDateBetween(Long accountId, LocalDate startDate, LocalDate endDate) {
        return jpaEntryRepository.sumAmountByAccountIdAndSettlementDateBetween(accountId, startDate, endDate);
    }

    @Override
    public Optional<LocalDate> findLatestSettlementDateByAccountId(Long accountId) {
        return Optional.ofNullable(jpaEntryRepository.findLatestSettlementDateByAccountId(accountId));
    }

    @Override
    public boolean existsDuplicate(
        Long accountId,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        Long categoryId,
        BigDecimal amount,
        String externalId
    ) {
        return jpaEntryRepository.existsByAccountIdAndMovementDateAndSettlementDateAndDescriptionAndCategoryIdAndAmountAndExternalId(
            accountId,
            movementDate,
            settlementDate,
            description,
            categoryId,
            amount,
            externalId
        );
    }

    @Override
    public boolean existsDuplicateIgnoringCategory(
        Long accountId,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        BigDecimal amount,
        String externalId
    ) {
        return jpaEntryRepository.existsDuplicateIgnoringCategory(
            accountId,
            movementDate,
            settlementDate,
            description,
            amount,
            externalId
        );
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

    private String normalizeDescriptionFilter(String value) {
        if (value == null) {
            return null;
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");

        return escapeLikePattern(normalized).toUpperCase(Locale.ROOT);
    }

    private String escapeLikePattern(String value) {
        if (value == null) {
            return null;
        }

        return value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
