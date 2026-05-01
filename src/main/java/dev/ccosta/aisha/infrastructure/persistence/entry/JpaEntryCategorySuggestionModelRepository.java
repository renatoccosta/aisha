package dev.ccosta.aisha.infrastructure.persistence.entry;

import dev.ccosta.aisha.domain.entry.categorization.model.EntryCategorySuggestionModelArtifact;
import dev.ccosta.aisha.domain.entry.categorization.model.EntryCategorySuggestionModelStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaEntryCategorySuggestionModelRepository extends JpaRepository<EntryCategorySuggestionModelArtifact, Long> {

    Optional<EntryCategorySuggestionModelArtifact> findTopByOrderByVersionDesc();

    Optional<EntryCategorySuggestionModelArtifact> findTopByStatusOrderByVersionDesc(EntryCategorySuggestionModelStatus status);

    @Query("select coalesce(max(m.version), 0) from EntryCategorySuggestionModelArtifact m")
    long findLatestVersion();
}
