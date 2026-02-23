package dev.ccosta.aisha.infrastructure.persistence.entry;

import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class EntryRepositoryAdapter implements EntryRepository {

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
        boolean onlyWithoutCategory,
        int page,
        int pageSize
    ) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        Page<Entry> result;

        if (onlyWithoutCategory) {
            if (accountId != null) {
                result = jpaEntryRepository.findBySettlementDateBetweenAndAccountIdAndCategoryIsNullOrderBySettlementDateDescIdDesc(
                    startDate,
                    endDate,
                    accountId,
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

            result = jpaEntryRepository.findBySettlementDateBetweenAndCategoryIsNullOrderBySettlementDateDescIdDesc(
                startDate,
                endDate,
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

        if (accountId != null && categoryId != null) {
            result = jpaEntryRepository.findBySettlementDateBetweenAndAccountIdAndCategoryIdOrderBySettlementDateDescIdDesc(
                startDate,
                endDate,
                accountId,
                categoryId,
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

        if (accountId != null) {
            result = jpaEntryRepository.findBySettlementDateBetweenAndAccountIdOrderBySettlementDateDescIdDesc(
                startDate,
                endDate,
                accountId,
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

        if (categoryId != null) {
            result = jpaEntryRepository.findBySettlementDateBetweenAndCategoryIdOrderBySettlementDateDescIdDesc(
                startDate,
                endDate,
                categoryId,
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

        result = jpaEntryRepository.findBySettlementDateBetweenOrderBySettlementDateDescIdDesc(startDate, endDate, pageRequest);
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
