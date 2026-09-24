-- CU 5 passo 04 e fluxo 4.1: o registro do usuário é removido; o histórico de
-- empréstimos e reservas encerrados permanece, sem o vínculo com o usuário.
-- Movimentações ativas continuam impedindo a exclusão, agora também no banco.

ALTER TABLE emprestimo ALTER COLUMN usuario_id DROP NOT NULL;
ALTER TABLE reserva    ALTER COLUMN usuario_id DROP NOT NULL;

ALTER TABLE emprestimo DROP CONSTRAINT fk_emprestimo_usuario;
ALTER TABLE emprestimo ADD CONSTRAINT fk_emprestimo_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE SET NULL;

ALTER TABLE emprestimo DROP CONSTRAINT fk_emprestimo_admin;
ALTER TABLE emprestimo ADD CONSTRAINT fk_emprestimo_admin
    FOREIGN KEY (registrado_por) REFERENCES usuario (id) ON DELETE SET NULL;

ALTER TABLE reserva DROP CONSTRAINT fk_reserva_usuario;
ALTER TABLE reserva ADD CONSTRAINT fk_reserva_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE SET NULL;

ALTER TABLE reserva DROP CONSTRAINT fk_reserva_admin;
ALTER TABLE reserva ADD CONSTRAINT fk_reserva_admin
    FOREIGN KEY (registrado_por) REFERENCES usuario (id) ON DELETE SET NULL;

-- Última linha de defesa do CU 5, fluxo 4.1: o SET NULL de uma movimentação
-- ativa viola estas regras e desfaz a exclusão.
ALTER TABLE emprestimo ADD CONSTRAINT chk_emprestimo_usuario_ativo
    CHECK (usuario_id IS NOT NULL OR status = 'CONCLUIDO');

ALTER TABLE reserva ADD CONSTRAINT chk_reserva_usuario_ativo
    CHECK (usuario_id IS NOT NULL OR status NOT IN ('AGUARDANDO', 'DISPONIVEL'));
