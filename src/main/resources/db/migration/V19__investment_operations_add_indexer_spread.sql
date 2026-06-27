ALTER TABLE investment_assets
    DROP COLUMN indexer_spread;

ALTER TABLE investment_assets
    ADD COLUMN indexer_spread NUMERIC(9, 6);

ALTER TABLE investment_operations
    ADD COLUMN indexer_spread NUMERIC(9, 6);
