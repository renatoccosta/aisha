package dev.ccosta.aisha.domain.entry.categorization.model;

import java.util.Optional;

/**
 * Stores the persisted versions of the entry category suggestion model.
 */
public interface EntryCategorySuggestionModelRepository {

    /**
     * Returns the most recent persisted model version regardless of status.
     *
     * @return the latest persisted model version, when available
     */
    Optional<EntryCategorySuggestionModelArtifact> findLatest();

    /**
     * Returns the most recent ready-to-use persisted model version.
     *
     * @return the latest ready model version, when available
     */
    Optional<EntryCategorySuggestionModelArtifact> findLatestReady();

    /**
     * Returns the next logical version number for a new training run.
     *
     * @return the next model version number
     */
    long nextVersion();

    /**
     * Persists the provided model artifact.
     *
     * @param artifact the artifact to persist
     * @return the persisted artifact
     */
    EntryCategorySuggestionModelArtifact save(EntryCategorySuggestionModelArtifact artifact);
}
