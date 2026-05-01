ALTER TABLE investment_operation_entries
    ADD CONSTRAINT uk_investment_operation_entries_operation
    UNIQUE (operation_id);

ALTER TABLE investment_operation_entries
    ADD CONSTRAINT uk_investment_operation_entries_entry
    UNIQUE (entry_id);
