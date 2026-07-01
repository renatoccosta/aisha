ALTER TABLE investment_operations
    ADD COLUMN external_id VARCHAR(255);

CREATE UNIQUE INDEX uk_investment_operations_external_id
    ON investment_operations (external_id);
