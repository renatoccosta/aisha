ALTER TABLE accounts
    ADD COLUMN account_type VARCHAR(30) DEFAULT 'OTHER';

UPDATE accounts
SET account_type = 'OTHER'
WHERE account_type IS NULL;

ALTER TABLE accounts
    ALTER COLUMN account_type SET NOT NULL;
