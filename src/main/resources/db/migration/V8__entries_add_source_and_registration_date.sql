ALTER TABLE entries
    ADD COLUMN entry_source VARCHAR(20);

ALTER TABLE entries
    ADD COLUMN registration_date DATE;
