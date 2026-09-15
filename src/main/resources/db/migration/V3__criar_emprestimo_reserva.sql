-- Casos de Uso 10 (Emprestimo), 11 (Reserva) e 12 (Devolucao)

CREATE TABLE emprestimo (
    id                BIGSERIAL     PRIMARY KEY,
    codigo            VARCHAR(20)   NOT NULL UNIQUE,
    usuario_id        BIGINT        NOT NULL,
    item_id           BIGINT        NOT NULL,
    data_emprestimo   DATE          NOT NULL DEFAULT CURRENT_DATE,
    data_prevista     DATE          NOT NULL,
    data_devolucao    DATE,
    status            VARCHAR(20)   NOT NULL DEFAULT 'EM_ANDAMENTO',
    dias_atraso       INTEGER       NOT NULL DEFAULT 0,
    valor_multa       NUMERIC(10,2) NOT NULL DEFAULT 0,
    multa_paga        BOOLEAN       NOT NULL DEFAULT FALSE,
    registrado_por    BIGINT,

    CONSTRAINT fk_emprestimo_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_emprestimo_item
        FOREIGN KEY (item_id) REFERENCES item (id),
    CONSTRAINT fk_emprestimo_admin
        FOREIGN KEY (registrado_por) REFERENCES usuario (id),

    CONSTRAINT chk_emprestimo_status
        CHECK (status IN ('EM_ANDAMENTO', 'ATRASADO', 'CONCLUIDO')),

    CONSTRAINT chk_emprestimo_prazo
        CHECK (data_prevista >= data_emprestimo),

    -- CU 12 passo 06: devolucao so existe em emprestimo concluido
    CONSTRAINT chk_emprestimo_devolucao
        CHECK (
            (status = 'CONCLUIDO' AND data_devolucao IS NOT NULL)
            OR (status <> 'CONCLUIDO' AND data_devolucao IS NULL)
        ),

    CONSTRAINT chk_emprestimo_atraso
        CHECK (dias_atraso >= 0 AND valor_multa >= 0)
);

CREATE INDEX idx_emprestimo_usuario ON emprestimo (usuario_id);
CREATE INDEX idx_emprestimo_item    ON emprestimo (item_id);
CREATE INDEX idx_emprestimo_status  ON emprestimo (status);

-- CU 10 fluxo 3.1: o mesmo exemplar nao pode estar emprestado duas vezes
CREATE UNIQUE INDEX uq_emprestimo_ativo
    ON emprestimo (usuario_id, item_id)
    WHERE status <> 'CONCLUIDO';


CREATE TABLE reserva (
    id              BIGSERIAL   PRIMARY KEY,
    usuario_id      BIGINT      NOT NULL,
    item_id         BIGINT      NOT NULL,
    data_reserva    DATE        NOT NULL DEFAULT CURRENT_DATE,
    validade_maxima DATE        NOT NULL,
    posicao_fila    INTEGER     NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'AGUARDANDO',
    registrado_por  BIGINT,

    CONSTRAINT fk_reserva_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_reserva_item
        FOREIGN KEY (item_id) REFERENCES item (id),
    CONSTRAINT fk_reserva_admin
        FOREIGN KEY (registrado_por) REFERENCES usuario (id),

    CONSTRAINT chk_reserva_status
        CHECK (status IN ('AGUARDANDO', 'DISPONIVEL', 'ATENDIDA', 'EXPIRADA', 'CANCELADA')),

    CONSTRAINT chk_reserva_posicao
        CHECK (posicao_fila > 0),

    CONSTRAINT chk_reserva_validade
        CHECK (validade_maxima >= data_reserva)
);

CREATE INDEX idx_reserva_item   ON reserva (item_id, posicao_fila);
CREATE INDEX idx_reserva_status ON reserva (status);

-- CU 11 fluxo 5.1: reserva duplicada e bloqueada pelo proprio banco
CREATE UNIQUE INDEX uq_reserva_ativa
    ON reserva (usuario_id, item_id)
    WHERE status IN ('AGUARDANDO', 'DISPONIVEL');
