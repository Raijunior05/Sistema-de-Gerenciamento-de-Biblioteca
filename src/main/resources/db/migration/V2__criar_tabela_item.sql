-- Caso de Uso 7 - Cadastrar Item / Caso de Uso 8 - Pesquisar Acervo

CREATE TABLE item (
    id                BIGSERIAL    PRIMARY KEY,
    tombo             VARCHAR(20)  NOT NULL UNIQUE,
    titulo            VARCHAR(250) NOT NULL,
    autor             VARCHAR(200) NOT NULL,
    isbn              VARCHAR(20)  UNIQUE,
    editora           VARCHAR(150),
    ano_publicacao    INTEGER,
    categoria         VARCHAR(80),
    tipo              VARCHAR(20)  NOT NULL DEFAULT 'LIVRO',
    quantidade_total  INTEGER      NOT NULL,
    quantidade_disp   INTEGER      NOT NULL,
    criado_em         TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_item_tipo
        CHECK (tipo IN ('LIVRO', 'REVISTA', 'PERIODICO', 'MIDIA', 'OUTRO')),

    CONSTRAINT chk_item_quantidade_total
        CHECK (quantidade_total >= 0),

    -- CU 10 passo 09 e CU 12 passo 07: a disponibilidade nunca sai do intervalo
    CONSTRAINT chk_item_quantidade_disp
        CHECK (quantidade_disp >= 0 AND quantidade_disp <= quantidade_total),

    CONSTRAINT chk_item_ano
        CHECK (ano_publicacao IS NULL OR ano_publicacao BETWEEN 1400 AND 2200)
);

CREATE INDEX idx_item_titulo    ON item (LOWER(titulo));
CREATE INDEX idx_item_autor     ON item (LOWER(autor));
CREATE INDEX idx_item_categoria ON item (categoria);

COMMENT ON COLUMN item.tombo IS 'Codigo interno gerado pelo sistema - CU 7 passo 07';
