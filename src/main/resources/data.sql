DELETE FROM entries;

INSERT INTO entries (account, movement_date, settlement_date, description, category, notes, amount) VALUES
('Conta Corrente Nubank', DATE '2026-02-01', DATE '2026-02-01', 'Salário mensal', 'Renda', 'Crédito de salário', 8500.00),
('Conta Corrente Nubank', DATE '2026-02-02', DATE '2026-02-02', 'Aluguel', 'Moradia', 'Pagamento via TED', -2200.00),
('Cartão Inter', DATE '2026-02-03', DATE '2026-02-03', 'Supermercado', 'Alimentação', 'Compra semanal', -485.73),
('Conta Corrente Nubank', DATE '2026-02-04', DATE '2026-02-04', 'Internet residencial', 'Serviços', 'Fatura mensal', -129.90),
('Conta Corrente Nubank', DATE '2026-02-05', DATE '2026-02-05', 'Energia elétrica', 'Serviços', 'Conta de luz', -214.66),
('Carteira', DATE '2026-02-06', DATE '2026-02-06', 'Almoço', 'Alimentação', 'Restaurante', -42.50),
('Conta Corrente Nubank', DATE '2026-02-07', DATE '2026-02-07', 'Investimento CDB', 'Investimentos', 'Aplicação mensal', -1000.00),
('Conta Corrente Nubank', DATE '2026-02-08', DATE '2026-02-08', 'Transferência recebida', 'Transferências', 'Reembolso de viagem', 320.00),
('Cartão Inter', DATE '2026-02-09', DATE '2026-02-09', 'Combustível', 'Transporte', 'Abastecimento', -260.18),
('Conta Corrente Nubank', DATE '2026-02-10', DATE '2026-02-10', 'Consulta médica', 'Saúde', 'Clínica particular', -180.00);
