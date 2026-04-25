ALTER TABLE investment_operations ADD COLUMN account_id BIGINT;

UPDATE investment_operations
SET account_id = (
    SELECT account_id
    FROM investment_assets
    WHERE investment_assets.id = investment_operations.asset_id
);

ALTER TABLE investment_operations ALTER COLUMN account_id SET NOT NULL;

ALTER TABLE investment_operations ADD CONSTRAINT fk_investment_operations_account
    FOREIGN KEY (account_id)
    REFERENCES accounts (id);

CREATE INDEX idx_investment_operations_account_id ON investment_operations (account_id);

ALTER TABLE investment_assets DROP CONSTRAINT fk_investment_assets_account;
DROP INDEX idx_investment_assets_account_id;
ALTER TABLE investment_assets DROP COLUMN account_id;
