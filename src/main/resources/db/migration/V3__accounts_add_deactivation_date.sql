ALTER TABLE accounts
    ADD COLUMN deactivation_date DATE;

CREATE INDEX idx_accounts_deactivation_date ON accounts (deactivation_date);
