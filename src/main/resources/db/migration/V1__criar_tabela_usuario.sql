-- Caso de Uso 3 - Cadastrar Usuario
-- Ator Usuario (ex-Leitor). O perfil ADMINISTRADOR e o unico que faz login.

CREATE TABLE usuario (
    id          BIGSERIAL    PRIMARY KEY,
    nome        VARCHAR(150) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    cpf         VARCHAR(14)  UNIQUE,
    matricula   VARCHAR(30)  UNIQUE,
    telefone    VARCHAR(20),
    nascimento  DATE,
    perfil      VARCHAR(20)  NOT NULL,
    login       VARCHAR(50)  UNIQUE,
    senha_hash  CHAR(60),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_usuario_perfil
        CHECK (perfil IN ('ADMINISTRADOR', 'USUARIO')),

    -- CU 9: a identificacao do Usuario e feita por CPF ou matricula
    CONSTRAINT chk_usuario_identificacao
        CHECK (cpf IS NOT NULL OR matricula IS NOT NULL),

    -- CU 1: somente o Administrador possui credenciais de acesso
    CONSTRAINT chk_usuario_credenciais
        CHECK (
            (perfil = 'ADMINISTRADOR' AND senha_hash IS NOT NULL)
            OR perfil = 'USUARIO'
        )
);

CREATE INDEX idx_usuario_nome ON usuario (LOWER(nome));
CREATE INDEX idx_usuario_cpf  ON usuario (cpf);
CREATE INDEX idx_usuario_mat  ON usuario (matricula);

COMMENT ON TABLE  usuario            IS 'Usuarios da biblioteca (CU 3 a 6)';
COMMENT ON COLUMN usuario.senha_hash IS 'Hash BCrypt de 60 caracteres - RNF-05';
