package dev.ccosta.aisha.infrastructure.persistence.entry;

import dev.ccosta.aisha.domain.entry.categorization.model.EntryCategorySuggestionModelArtifact;
import dev.ccosta.aisha.domain.entry.categorization.model.EntryCategorySuggestionModelRepository;
import dev.ccosta.aisha.domain.entry.categorization.model.EntryCategorySuggestionModelStatus;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * JPA-backed repository for persisted entry category suggestion model versions.
 */
@Repository
public class EntryCategorySuggestionModelRepositoryAdapter implements EntryCategorySuggestionModelRepository {

    private final JpaEntryCategorySuggestionModelRepository jpaRepository;

    public EntryCategorySuggestionModelRepositoryAdapter(JpaEntryCategorySuggestionModelRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<EntryCategorySuggestionModelArtifact> findLatest() {
        return jpaRepository.findTopByOrderByVersionDesc();
    }

    @Override
    public Optional<EntryCategorySuggestionModelArtifact> findLatestReady() {
        return jpaRepository.findTopByStatusOrderByVersionDesc(EntryCategorySuggestionModelStatus.READY);
    }

    @Override
    public long nextVersion() {
        return jpaRepository.findLatestVersion() + 1;
    }

    @Override
    public EntryCategorySuggestionModelArtifact save(EntryCategorySuggestionModelArtifact artifact) {
        return jpaRepository.save(artifact);
    }
}
