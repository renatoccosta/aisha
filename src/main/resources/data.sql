DELETE FROM entries;
DELETE FROM categories;

INSERT INTO categories (title, description, parent_id) VALUES
('Renda', 'Entradas financeiras', NULL),
('Moradia', 'Gastos com habitação', NULL),
('Alimentação', 'Gastos alimentares', NULL),
('Serviços', 'Serviços recorrentes', NULL),
('Investimentos', 'Aplicações financeiras', NULL),
('Transferências', 'Transferências entre contas', NULL),
('Transporte', 'Mobilidade e deslocamentos', NULL),
('Saúde', 'Despesas com saúde', NULL),
('Lazer', 'Gastos de lazer', NULL),
('Restaurantes', 'Refeições fora de casa', (SELECT id FROM categories WHERE title = 'Alimentação'));

INSERT INTO entries (account, movement_date, settlement_date, description, category_id, notes, amount) VALUES
('Conta Corrente Nubank', DATE '2026-02-01', DATE '2026-02-01', 'Salário mensal', (SELECT id FROM categories WHERE title = 'Renda'), 'Crédito de salário', 8500.00),
('Conta Corrente Nubank', DATE '2026-02-02', DATE '2026-02-02', 'Aluguel', (SELECT id FROM categories WHERE title = 'Moradia'), 'Pagamento via TED', -2200.00),
('Cartão Inter', DATE '2026-02-03', DATE '2026-02-03', 'Supermercado', (SELECT id FROM categories WHERE title = 'Alimentação'), 'Compra semanal', -485.73),
('Conta Corrente Nubank', DATE '2026-02-04', DATE '2026-02-04', 'Internet residencial', (SELECT id FROM categories WHERE title = 'Serviços'), 'Fatura mensal', -129.90),
('Conta Corrente Nubank', DATE '2026-02-05', DATE '2026-02-05', 'Energia elétrica', (SELECT id FROM categories WHERE title = 'Serviços'), 'Conta de luz', -214.66),
('Carteira', DATE '2026-02-06', DATE '2026-02-06', 'Almoço', (SELECT id FROM categories WHERE title = 'Restaurantes'), 'Restaurante', -42.50),
('Conta Corrente Nubank', DATE '2026-02-07', DATE '2026-02-07', 'Investimento CDB', (SELECT id FROM categories WHERE title = 'Investimentos'), 'Aplicação mensal', -1000.00),
('Conta Corrente Nubank', DATE '2026-02-08', DATE '2026-02-08', 'Transferência recebida', (SELECT id FROM categories WHERE title = 'Transferências'), 'Reembolso de viagem', 320.00),
('Cartão Inter', DATE '2026-02-09', DATE '2026-02-09', 'Combustível', (SELECT id FROM categories WHERE title = 'Transporte'), 'Abastecimento', -260.18),
('Conta Corrente Nubank', DATE '2026-02-10', DATE '2026-02-10', 'Consulta médica', (SELECT id FROM categories WHERE title = 'Saúde'), 'Clínica particular', -180.00);
