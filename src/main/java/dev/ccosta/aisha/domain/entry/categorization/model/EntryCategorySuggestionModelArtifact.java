package dev.ccosta.aisha.domain.entry.categorization.model;

import dev.ccosta.aisha.application.entry.categorization.model.EntryCategoryModelTrainingTrigger;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Persists the training history and payload of entry category suggestion model versions.
 */
@Entity
@Table(name = "entry_category_suggestion_models")
public class EntryCategorySuggestionModelArtifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version", nullable = false, unique = true)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EntryCategorySuggestionModelStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_source", nullable = false, length = 20)
    private EntryCategoryModelTrainingTrigger triggerSource;

    @Column(name = "model_name", nullable = false, length = 120)
    private String modelName;

    @Column(name = "pipeline_version", nullable = false, length = 80)
    private String pipelineVersion;

    @Column(name = "training_example_count")
    private Integer trainingExampleCount;

    @Column(name = "label_count")
    private Integer labelCount;

    @Column(name = "vocabulary_size")
    private Integer vocabularySize;

    @Column(name = "training_started_at", nullable = false)
    private LocalDateTime trainingStartedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "model_payload", length = 1048576)
    private String modelPayload;

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public EntryCategorySuggestionModelStatus getStatus() {
        return status;
    }

    public void setStatus(EntryCategorySuggestionModelStatus status) {
        this.status = status;
    }

    public EntryCategoryModelTrainingTrigger getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(EntryCategoryModelTrainingTrigger triggerSource) {
        this.triggerSource = triggerSource;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPipelineVersion() {
        return pipelineVersion;
    }

    public void setPipelineVersion(String pipelineVersion) {
        this.pipelineVersion = pipelineVersion;
    }

    public Integer getTrainingExampleCount() {
        return trainingExampleCount;
    }

    public void setTrainingExampleCount(Integer trainingExampleCount) {
        this.trainingExampleCount = trainingExampleCount;
    }

    public Integer getLabelCount() {
        return labelCount;
    }

    public void setLabelCount(Integer labelCount) {
        this.labelCount = labelCount;
    }

    public Integer getVocabularySize() {
        return vocabularySize;
    }

    public void setVocabularySize(Integer vocabularySize) {
        this.vocabularySize = vocabularySize;
    }

    public LocalDateTime getTrainingStartedAt() {
        return trainingStartedAt;
    }

    public void setTrainingStartedAt(LocalDateTime trainingStartedAt) {
        this.trainingStartedAt = trainingStartedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public String getModelPayload() {
        return modelPayload;
    }

    public void setModelPayload(String modelPayload) {
        this.modelPayload = modelPayload;
    }
}
