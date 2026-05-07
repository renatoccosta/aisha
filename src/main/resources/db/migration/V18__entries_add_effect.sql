ALTER TABLE entries
    ADD COLUMN entry_effect VARCHAR(20) DEFAULT 'RESULT';

UPDATE entries
SET entry_effect = 'RESULT'
WHERE entry_effect IS NULL;

UPDATE entries
SET entry_effect = 'EQUITY',
    category_id = NULL,
    suggested_category_id = NULL,
    category_suggestion_confidence = NULL,
    category_suggestion_status = 'NONE'
WHERE entry_type = 'TRANSFER'
   OR id IN (
        SELECT net_entry_id
        FROM brokerage_notes
   );

ALTER TABLE entries
    ALTER COLUMN entry_effect SET NOT NULL;

ALTER TABLE entries
    ALTER COLUMN entry_effect SET DEFAULT 'RESULT';

CREATE INDEX idx_entries_entry_effect ON entries (entry_effect);
