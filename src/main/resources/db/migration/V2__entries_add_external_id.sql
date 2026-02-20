ALTER TABLE entries
    ADD COLUMN external_id VARCHAR(255);

CREATE INDEX idx_entries_external_id ON entries (external_id);
