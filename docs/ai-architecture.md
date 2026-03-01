# AI Architecture

This document describes the current AI architecture in AI$HA and the first implemented AI use case: automatic entry category suggestion.

## Objectives

- Keep AI features auditable and easy to evolve.
- Isolate machine learning concerns from web and persistence details.
- Ensure user feedback is preserved as explicit training signal.
- Support future AI use cases without coupling them to entry categorization specifics.

## Architectural Overview

The current design introduces a small, layered AI architecture:

- `application/ai/classification`
  - Generic contracts for classification-oriented AI use cases.
  - Defines the application-facing abstractions for predictions, requests, and training examples.
- `infrastructure/ai/smile`
  - Concrete ML implementation based on the SMILE library.
  - Currently provides a Naive Bayes text classifier.
- `application/entry`
  - Entry-specific orchestration for category suggestion.
  - Converts domain history into training examples and converts classifier output into category suggestions.

This separation keeps the project open for future capabilities such as anomaly detection, cash flow prediction, or document classification without reworking the entry module.

## Current Packages and Responsibilities

- `src/main/java/dev/ccosta/aisha/application/ai/classification`
  - `TextClassifier`
  - `TextClassificationRequest`
  - `TextClassificationExample`
  - `ClassificationPrediction`
- `src/main/java/dev/ccosta/aisha/infrastructure/ai/smile`
  - `SmileNaiveBayesTextClassifier`
- `src/main/java/dev/ccosta/aisha/application/entry`
  - `EntryCategorySuggestionService`
  - `EntryCategorySuggestion`
  - `EntryCategorySuggestionRequest`
  - `EntryCategorySelection`

## Why This Design

The design intentionally avoids a direct dependency from controllers or repositories to SMILE:

- Web controllers depend on application services only.
- Entry services work with entry-specific suggestion data, not ML library APIs.
- The SMILE-based implementation is replaceable if a different model or library is needed later.

This allows the codebase to evolve from a single classifier to multiple AI strategies while preserving package boundaries.

## Category Suggestion Use Case

The first AI use case is supervised classification of entry categories.

The goal is to suggest one of the existing categories based on historical user behavior.

### Input Signals

The current suggestion flow uses:

- account id
- entry description
- amount sign
- amount bucket

The description is treated as the main text signal. Account and amount-derived attributes are injected as contextual tokens.

### Training Data Source

Training data is extracted from existing entries using `EntryRepository.listCategoryTrainingExamples()`.

Each training example contains:

- account id
- description
- amount
- final category id

Only entries that represent validated user choices are used as training signal. Entries with `PENDING` suggestion status are excluded from training.

This prevents unconfirmed imported suggestions from reinforcing themselves.

### Model

Current model:

- Library: SMILE
- Implementation: `DiscreteNaiveBayes`
- Model type: multinomial Naive Bayes

This model was chosen because it is:

- simple
- fast for retraining on each request
- appropriate for text classification baselines
- easy to reason about and audit

### Feature Construction

The classifier tokenizes normalized description text and appends contextual tokens such as:

- `account-{id}`
- `kind-expense` or `kind-income`
- amount bucket tokens such as `bucket-small`, `bucket-medium`, and so on

This produces a compact bag-of-words style representation.

## User Feedback Loop

The solution captures feedback in two ways.

### Manual Entry Creation or Edit

During manual entry:

1. The user fills account, description, and amount.
2. The system requests a suggestion through HTMX.
3. The suggested category is preselected in the form.
4. If the user saves without changing the suggested category, the suggestion is stored as `ACCEPTED`.
5. If the user changes the category before saving, the suggestion is stored as `REJECTED`.

This makes manual entry a direct supervised feedback channel.

### Statement Import

During statement import:

1. The imported record is parsed.
2. The category suggestion service is invoked.
3. If a category is suggested, it is applied to the imported entry.
4. The entry is stored with status `PENDING`.

Because import is not interactive, the system does not assume that the suggested category was validated by the user.

Later, the user can:

- filter entries with pending suggestions
- confirm the suggested category directly
- edit the entry and change the category, producing a rejection signal

## Persistence Model

The `entries` table now stores AI-related metadata:

- `suggested_category_id`
- `category_suggestion_status`
- `category_suggestion_confidence`

These fields allow the application to distinguish:

- entries with no AI involvement
- entries with pending imported suggestions
- accepted suggestions
- rejected suggestions

The corresponding domain enum is `EntryCategorySuggestionStatus`:

- `NONE`
- `PENDING`
- `ACCEPTED`
- `REJECTED`

## End-to-End Flow

### Manual Suggestion Flow

1. Browser edits the entry form.
2. HTMX calls `/entries/fragments/category-suggestion`.
3. `EntryController` builds `EntryCategorySuggestionRequest`.
4. `EntryCategorySuggestionService` gathers training examples from the repository.
5. The generic classifier delegates to the SMILE implementation.
6. The suggested category is rendered back into the form fragment.
7. On save, `EntryService` persists the final category and the feedback outcome.

### Import Suggestion Flow

1. `EntryStatementImportService` parses statement records.
2. For each record, it calls `EntryCategorySuggestionService`.
3. If a suggestion exists, the entry receives:
   - `category = suggested category`
   - `suggestedCategory = suggested category`
   - `categorySuggestionStatus = PENDING`
4. The user later validates these entries from the listing UI.

## Filtering and Review

The entries listing now supports filtering for pending suggestions.

This review capability is important for two reasons:

- it gives the user a practical validation queue after imports
- it creates a controlled path to convert `PENDING` data into validated training data

## Safety and Auditability

The implementation uses conservative rules:

- only existing categories can be suggested
- training excludes unvalidated imported suggestions
- confidence is stored for inspection
- acceptance and rejection are explicitly persisted
- AI output does not bypass normal domain validation

This keeps the feature explainable and reduces the risk of silent data drift.

## Current Limitations

- The model is retrained in-process on demand from historical data.
- Suggestion quality depends heavily on description consistency and category history volume.
- Confidence is relative to the current simple model and should not be treated as a calibrated probability.
- The current implementation suggests a single category only.
- There is no separate background training pipeline yet.

These tradeoffs are acceptable for the current stage because the implementation remains simple, deterministic, and easy to evolve.

## Evolution Paths

The current architecture was designed to support future improvements such as:

- alternative classifiers behind the same `TextClassifier` contract
- richer feature engineering
- model selection per use case
- cached or scheduled model training
- additional AI modules under `application/ai`
- review dashboards and suggestion quality metrics

If AI use cases expand significantly, a next step would be introducing a dedicated `application/ai` orchestration layer for model lifecycle concerns such as training policies, model versioning, and offline evaluation.
