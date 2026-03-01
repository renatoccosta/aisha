ALTER TABLE entries
    ADD COLUMN suggested_category_id BIGINT;

ALTER TABLE entries
    ADD COLUMN category_suggestion_status VARCHAR(20) DEFAULT 'NONE' NOT NULL;

ALTER TABLE entries
    ADD COLUMN category_suggestion_confidence DOUBLE PRECISION;

ALTER TABLE entries
    ADD CONSTRAINT fk_entries_suggested_category
        FOREIGN KEY (suggested_category_id)
        REFERENCES categories (id);

CREATE INDEX idx_entries_category_suggestion_status ON entries (category_suggestion_status);
CREATE INDEX idx_entries_suggested_category_id ON entries (suggested_category_id);
