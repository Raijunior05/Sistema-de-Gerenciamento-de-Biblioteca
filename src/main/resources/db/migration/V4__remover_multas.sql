-- CU 10 passo 07 e CU 12 fluxo 5.1: controle de atraso sem cobrança.
-- Preserva empréstimos, datas e dias de atraso; descarta os campos financeiros legados.
ALTER TABLE emprestimo DROP CONSTRAINT chk_emprestimo_atraso;
ALTER TABLE emprestimo
    DROP COLUMN valor_multa,
    DROP COLUMN multa_paga;
ALTER TABLE emprestimo
    ADD CONSTRAINT chk_emprestimo_atraso CHECK (dias_atraso >= 0);
