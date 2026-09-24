-- =====================================================================
-- BiblioTech - dados de demonstração
--
-- Cria usuários, acervo, empréstimos (em dia, atrasados e concluídos) e
-- reservas (fila, exemplar separado para o 1º da fila e histórico),
-- coerentes com as regras dos casos de uso. As datas são relativas ao dia
-- da execução, para a demonstração parecer sempre atual.
--
-- Pode ser executado várias vezes: remove os dados de demonstração anteriores
-- (e-mails @demo.bibliotech.local, tombos e códigos DEM-) antes de recriá-los.
-- Não altera os demais dados do banco.
--
-- Pré-requisito: abrir a aplicação uma vez para o Flyway criar o schema.
-- Execução: scripts/seed-demo.ps1 (Windows) ou scripts/seed-demo.sh.
-- Prazo de empréstimo usado: 15 dias (regra.diasPrazoEmprestimo padrão).
-- Validade de reserva usada: 7 dias (regra.diasValidadeReserva padrão).
-- =====================================================================

\set ON_ERROR_STOP on

DO $$
BEGIN
    IF to_regclass('public.emprestimo') IS NULL THEN
        RAISE EXCEPTION 'Schema inexistente: abra a aplicação uma vez para o Flyway criar as tabelas.';
    END IF;
END $$;

BEGIN;

-- ---------------------------------------------------------------------
-- 1. Limpeza da demonstração anterior
-- ---------------------------------------------------------------------

-- Empréstimos abertos de itens reais feitos por usuários de demonstração
-- devolvem o exemplar ao acervo antes de serem apagados.
UPDATE item i
   SET quantidade_disp = quantidade_disp + abertos.qtd
  FROM (SELECT e.item_id, COUNT(*) AS qtd
          FROM emprestimo e
          JOIN usuario u ON u.id = e.usuario_id
         WHERE u.email LIKE '%@demo.bibliotech.local'
           AND e.status <> 'CONCLUIDO'
         GROUP BY e.item_id) abertos
 WHERE i.id = abertos.item_id
   AND i.tombo NOT LIKE 'DEM-%';

DELETE FROM reserva
 WHERE item_id IN (SELECT id FROM item WHERE tombo LIKE 'DEM-%')
    OR usuario_id IN (SELECT id FROM usuario WHERE email LIKE '%@demo.bibliotech.local');

DELETE FROM emprestimo
 WHERE codigo LIKE 'DEM-%'
    OR item_id IN (SELECT id FROM item WHERE tombo LIKE 'DEM-%')
    OR usuario_id IN (SELECT id FROM usuario WHERE email LIKE '%@demo.bibliotech.local');

DELETE FROM item WHERE tombo LIKE 'DEM-%';
DELETE FROM usuario WHERE email LIKE '%@demo.bibliotech.local';

-- ---------------------------------------------------------------------
-- 2. Usuários (CU 3)
-- ---------------------------------------------------------------------

-- Segundo Administrador: login "coordenacao", senha "demo123" (hash BCrypt, RNF-05).
INSERT INTO usuario (nome, email, cpf, matricula, telefone, nascimento, perfil, login, senha_hash, criado_em)
VALUES ('Coordenação da Biblioteca', 'coordenacao@demo.bibliotech.local', '321.654.987-00', NULL,
        '(87) 3862-0000', NULL, 'ADMINISTRADOR', 'coordenacao',
        '$2a$12$Fg77LlaozfdJPwBkpeX9yuVUbdv/RFTeaFGig61VfgwEdOJfukatm',
        CURRENT_DATE - 90);

INSERT INTO usuario (nome, email, cpf, matricula, telefone, nascimento, perfil, criado_em)
SELECT v.nome, v.email, v.cpf, v.matricula, v.telefone, v.nascimento::date, 'USUARIO',
       CURRENT_DATE + v.cadastro
  FROM (VALUES
    ('Carlos Alberto Souza',    'carlos.souza@demo.bibliotech.local',   '321.654.987-01', NULL,      '(87) 99911-0001', '1995-03-12', -120),
    ('Mariana Costa Silva',     'mariana.costa@demo.bibliotech.local',  '321.654.987-02', NULL,      '(87) 99911-0002', '1998-07-25', -110),
    ('Juliana Reis Mendes',     'juliana.reis@demo.bibliotech.local',   NULL,             '2023001', '(87) 99911-0003', '2000-11-03', -100),
    ('Amanda Siqueira Mendes',  'amanda.siqueira@demo.bibliotech.local','321.654.987-04', NULL,      '(87) 99911-0004', '1993-01-19',  -95),
    ('Bruno Oliveira Lopes',    'bruno.lopes@demo.bibliotech.local',    NULL,             '2023002', '(87) 99911-0005', '1999-05-30',  -80),
    ('Fernanda Rocha Dias',     'fernanda.rocha@demo.bibliotech.local', '321.654.987-06', NULL,      '(87) 99911-0006', '2001-02-14',  -60),
    ('Rafael Gomes Pereira',    'rafael.gomes@demo.bibliotech.local',   NULL,             '2024010', '(87) 99911-0007', '2002-09-08',  -50),
    ('Patrícia Nunes Araújo',   'patricia.nunes@demo.bibliotech.local', '321.654.987-08', '2024011', '(87) 99911-0008', '1997-12-01',  -45),
    ('Gabriela Torres Lima',    'gabriela.torres@demo.bibliotech.local','321.654.987-09', NULL,      '(87) 99911-0009', '1994-06-21',  -70),
    ('Lucas Martins Ferreira',  'lucas.martins@demo.bibliotech.local',  NULL,             '2025003', '(87) 99911-0010', '2003-04-17',   -3)
  ) AS v(nome, email, cpf, matricula, telefone, nascimento, cadastro);

-- ---------------------------------------------------------------------
-- 3. Acervo (CU 7); disponibilidade recalculada no passo 6
-- ---------------------------------------------------------------------

INSERT INTO item (tombo, titulo, autor, isbn, editora, ano_publicacao, categoria, tipo,
                  quantidade_total, quantidade_disp)
SELECT v.tombo, v.titulo, v.autor,
       -- ISBN já usado por item real fica em branco (coluna UNIQUE)
       CASE WHEN EXISTS (SELECT 1 FROM item x WHERE x.isbn = v.isbn) THEN NULL ELSE v.isbn END,
       v.editora, v.ano, v.categoria, v.tipo, v.total, v.total
  FROM (VALUES
    ('DEM-001', 'O Cortiço',                          'Aluísio Azevedo',      '9788572329972', 'Martin Claret',   1890, 'Literatura brasileira', 'LIVRO',     3),
    ('DEM-002', 'Dom Casmurro',                       'Machado de Assis',     '9788594318602', 'Principis',       1899, 'Literatura brasileira', 'LIVRO',     4),
    ('DEM-003', 'A Hora da Estrela',                  'Clarice Lispector',    '9788520937066', 'Rocco',           1977, 'Literatura brasileira', 'LIVRO',     2),
    ('DEM-004', 'O Hobbit',                           'J.R.R. Tolkien',       '9788595084742', 'HarperCollins',   1937, 'Fantasia',              'LIVRO',     2),
    ('DEM-005', '1984',                               'George Orwell',        '9788535914849', 'Companhia das Letras', 1949, 'Ficção científica', 'LIVRO',   2),
    ('DEM-006', 'Guerra e Paz',                       'Liev Tolstói',         '9788535922486', 'Companhia das Letras', 1869, 'Clássico',          'LIVRO',   1),
    ('DEM-007', 'Memórias Póstumas de Brás Cubas',    'Machado de Assis',     '9788544001820', 'Penguin',         1881, 'Literatura brasileira', 'LIVRO',     3),
    ('DEM-008', 'Vidas Secas',                        'Graciliano Ramos',     '9788501114516', 'Record',          1938, 'Literatura brasileira', 'LIVRO',     2),
    ('DEM-009', 'Capitães da Areia',                  'Jorge Amado',          '9788535914061', 'Companhia das Letras', 1937, 'Literatura brasileira', 'LIVRO', 2),
    ('DEM-010', 'Grande Sertão: Veredas',             'João Guimarães Rosa',  '9788535908084', 'Companhia das Letras', 1956, 'Literatura brasileira', 'LIVRO', 1),
    ('DEM-011', 'Código Limpo',                       'Robert C. Martin',     '9788576082675', 'Alta Books',      2009, 'Tecnologia',            'LIVRO',     2),
    ('DEM-012', 'Engenharia de Software',             'Ian Sommerville',      '9788543024974', 'Pearson',         2018, 'Tecnologia',            'LIVRO',     3),
    ('DEM-013', 'Sapiens: Uma Breve História da Humanidade', 'Yuval Noah Harari', '9788525432186', 'L&PM',      2015, 'História',              'LIVRO',     2),
    ('DEM-014', 'Revista Superinteressante - Ed. 450', 'Editora Abril',       NULL,            'Abril',           2023, 'Divulgação científica', 'REVISTA',   5),
    ('DEM-015', 'Central do Brasil (DVD)',            'Walter Salles',        NULL,            'Videofilmes',     1998, 'Cinema nacional',       'MIDIA',     1)
  ) AS v(tombo, titulo, autor, isbn, editora, ano, categoria, tipo, total);

-- ---------------------------------------------------------------------
-- 4. Empréstimos (CU 10 e CU 12)
--    retirada/devolucao: dias em relação a hoje; prazo = retirada + 15.
--    Sem devolução e prazo vencido -> ATRASADO com dias de atraso;
--    com devolução -> CONCLUIDO com os dias de atraso da devolução (fluxo 5.1).
-- ---------------------------------------------------------------------

INSERT INTO emprestimo (codigo, usuario_id, item_id, data_emprestimo, data_prevista,
                        data_devolucao, status, dias_atraso, registrado_por)
SELECT v.codigo, u.id, i.id,
       CURRENT_DATE + v.retirada,
       CURRENT_DATE + v.retirada + 15,
       CASE WHEN v.devolucao IS NULL THEN NULL ELSE CURRENT_DATE + v.devolucao END,
       CASE WHEN v.devolucao IS NOT NULL THEN 'CONCLUIDO'
            WHEN v.retirada + 15 < 0 THEN 'ATRASADO'
            ELSE 'EM_ANDAMENTO' END,
       CASE WHEN v.devolucao IS NOT NULL THEN GREATEST(v.devolucao - (v.retirada + 15), 0)
            ELSE GREATEST(-(v.retirada + 15), 0) END,
       adm.id
  FROM (VALUES
    -- Em andamento, no prazo
    ('DEM-0001', 'carlos.souza',    'DEM-002',  -3, NULL::int),
    ('DEM-0002', 'carlos.souza',    'DEM-011', -10, NULL),
    ('DEM-0003', 'carlos.souza',    'DEM-008',  -6, NULL),   -- Carlos atinge o limite de 3 (CU 10, 7.1)
    ('DEM-0004', 'mariana.costa',   'DEM-004',  -8, NULL),
    ('DEM-0005', 'juliana.reis',    'DEM-004',  -5, NULL),   -- O Hobbit fica sem exemplar
    ('DEM-0006', 'rafael.gomes',    'DEM-005',  -2, NULL),
    ('DEM-0007', 'patricia.nunes',  'DEM-012',  -1, NULL),
    ('DEM-0008', 'amanda.siqueira', 'DEM-013',  -4, NULL),
    -- Em aberto e atrasados (bloqueiam novo empréstimo: CU 10, 7.1)
    ('DEM-0009', 'mariana.costa',   'DEM-005', -25, NULL),   -- 10 dias; 1984 fica sem exemplar
    ('DEM-0010', 'bruno.lopes',     'DEM-001', -20, NULL),   -- 5 dias
    ('DEM-0011', 'patricia.nunes',  'DEM-007', -18, NULL),   -- 3 dias
    -- Concluídos (histórico e relatório de atrasos)
    ('DEM-0012', 'gabriela.torres', 'DEM-003', -40, -27),    -- no prazo
    ('DEM-0013', 'gabriela.torres', 'DEM-006', -30, -12),    -- 3 dias de atraso
    ('DEM-0014', 'carlos.souza',    'DEM-001', -60, -44),    -- 1 dia de atraso
    ('DEM-0015', 'amanda.siqueira', 'DEM-009', -35, -22),    -- no prazo
    ('DEM-0016', 'juliana.reis',    'DEM-010', -18,  -1),    -- 2 dias; exemplar separado p/ reserva
    ('DEM-0017', 'bruno.lopes',     'DEM-015', -50, -36),    -- no prazo
    ('DEM-0018', 'patricia.nunes',  'DEM-014', -16,  -4),    -- no prazo
    ('DEM-0019', 'rafael.gomes',    'DEM-002', -45, -28),    -- 2 dias de atraso
    ('DEM-0020', 'fernanda.rocha',  'DEM-012', -75, -55),    -- 5 dias de atraso
    ('DEM-0021', 'carlos.souza',    'DEM-007',  -9,  -2)     -- devolvido nesta semana
  ) AS v(codigo, email, tombo, retirada, devolucao)
  JOIN usuario u ON u.email = v.email || '@demo.bibliotech.local'
  JOIN item    i ON i.tombo = v.tombo
 CROSS JOIN (SELECT COALESCE(
                (SELECT id FROM usuario WHERE email = 'admin@bibliotech.local'),
                (SELECT id FROM usuario WHERE email = 'coordenacao@demo.bibliotech.local')) AS id) adm;

-- ---------------------------------------------------------------------
-- 5. Reservas (CU 11 e CU 12, fluxo 7.1)
--    validade = data da reserva + 7; a separada para retirada vale 7 dias
--    a partir da devolução que a liberou.
-- ---------------------------------------------------------------------

INSERT INTO reserva (usuario_id, item_id, data_reserva, validade_maxima, posicao_fila,
                     status, registrado_por)
SELECT u.id, i.id, CURRENT_DATE + v.reserva, CURRENT_DATE + v.validade, v.posicao, v.status, adm.id
  FROM (VALUES
    -- Fila de O Hobbit e de 1984 (itens sem exemplar)
    ('amanda.siqueira', 'DEM-004',  -4,  3, 1, 'AGUARDANDO'),
    ('bruno.lopes',     'DEM-004',  -1,  6, 2, 'AGUARDANDO'),
    ('carlos.souza',    'DEM-005',  -3,  4, 1, 'AGUARDANDO'),
    -- Grande Sertão devolvido ontem: exemplar separado para Fernanda (1º da fila)
    ('fernanda.rocha',  'DEM-010', -10,  6, 1, 'DISPONIVEL'),
    ('rafael.gomes',    'DEM-010',  -2,  5, 2, 'AGUARDANDO'),
    -- Histórico
    ('amanda.siqueira', 'DEM-009', -38, -31, 1, 'ATENDIDA'),
    ('bruno.lopes',     'DEM-006', -33, -26, 1, 'CANCELADA'),
    ('patricia.nunes',  'DEM-008', -30, -23, 1, 'EXPIRADA')
  ) AS v(email, tombo, reserva, validade, posicao, status)
  JOIN usuario u ON u.email = v.email || '@demo.bibliotech.local'
  JOIN item    i ON i.tombo = v.tombo
 CROSS JOIN (SELECT COALESCE(
                (SELECT id FROM usuario WHERE email = 'admin@bibliotech.local'),
                (SELECT id FROM usuario WHERE email = 'coordenacao@demo.bibliotech.local')) AS id) adm;

-- ---------------------------------------------------------------------
-- 6. Disponibilidade = total - empréstimos em aberto (CU 10 passo 09)
-- ---------------------------------------------------------------------

UPDATE item i
   SET quantidade_disp = i.quantidade_total - (
           SELECT COUNT(*) FROM emprestimo e
            WHERE e.item_id = i.id AND e.status <> 'CONCLUIDO')
 WHERE i.tombo LIKE 'DEM-%';

COMMIT;

-- ---------------------------------------------------------------------
-- Resumo
-- ---------------------------------------------------------------------

SELECT 'usuarios' AS dado, COUNT(*) AS total
  FROM usuario WHERE email LIKE '%@demo.bibliotech.local'
UNION ALL
SELECT 'itens', COUNT(*) FROM item WHERE tombo LIKE 'DEM-%'
UNION ALL
SELECT 'emprestimos em andamento', COUNT(*) FROM emprestimo
 WHERE codigo LIKE 'DEM-%' AND status = 'EM_ANDAMENTO'
UNION ALL
SELECT 'emprestimos atrasados', COUNT(*) FROM emprestimo
 WHERE codigo LIKE 'DEM-%' AND status = 'ATRASADO'
UNION ALL
SELECT 'emprestimos concluidos', COUNT(*) FROM emprestimo
 WHERE codigo LIKE 'DEM-%' AND status = 'CONCLUIDO'
UNION ALL
SELECT 'reservas ativas', COUNT(*) FROM reserva r
  JOIN item i ON i.id = r.item_id
 WHERE i.tombo LIKE 'DEM-%' AND r.status IN ('AGUARDANDO', 'DISPONIVEL');
