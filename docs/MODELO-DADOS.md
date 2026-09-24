# Modelo de dados

Quatro tabelas, criadas pelas migrations do Flyway em
`src/main/resources/db/migration`.

```
usuario ──┬──< emprestimo >──┬── item
          └──< reserva    >──┘
```

---

## usuario

Casos de uso 3, 4, 5, 6 e 9.

| Coluna | Tipo | Observação |
| --- | --- | --- |
| id | BIGSERIAL | PK |
| nome | VARCHAR(150) | NOT NULL |
| email | VARCHAR(150) | NOT NULL, UNIQUE |
| cpf | VARCHAR(14) | UNIQUE |
| matricula | VARCHAR(30) | UNIQUE |
| telefone | VARCHAR(20) | do protótipo |
| nascimento | DATE | do protótipo |
| perfil | VARCHAR(20) | ADMINISTRADOR ou USUARIO |
| login | VARCHAR(50) | UNIQUE, só para Administrador |
| senha_hash | CHAR(60) | BCrypt, só para Administrador |
| ativo | BOOLEAN | exclusão lógica opcional |

**Constraints**

- `chk_usuario_perfil` — restringe aos dois perfis da versão 3 do documento.
- `chk_usuario_identificacao` — exige CPF **ou** matrícula, viabilizando o CU 9.
- `chk_usuario_credenciais` — Administrador obrigatoriamente tem senha.

---

## item

Casos de uso 7 e 8.

| Coluna | Tipo | Observação |
| --- | --- | --- |
| id | BIGSERIAL | PK |
| tombo | VARCHAR(20) | UNIQUE, gerado no CU 7 passo 07 |
| titulo, autor | VARCHAR | NOT NULL |
| isbn | VARCHAR(20) | UNIQUE, opcional; em branco é gravado como nulo |
| editora, ano_publicacao, categoria | — | opcionais |
| tipo | VARCHAR(20) | LIVRO, REVISTA, PERIODICO, MIDIA, OUTRO |
| quantidade_total | INTEGER | exemplares do acervo |
| quantidade_disp | INTEGER | exemplares na prateleira |

**Constraints**

- `chk_item_quantidade_disp` — `0 <= disponível <= total`. É o que impede
  disponibilidade negativa no CU 10 passo 09 e estouro no CU 12 passo 07.

---

## emprestimo

Casos de uso 10 e 12.

| Coluna | Tipo | Observação |
| --- | --- | --- |
| id | BIGSERIAL | PK |
| codigo | VARCHAR(20) | UNIQUE, formato EMP-0000 |
| usuario_id | BIGINT | FK `ON DELETE SET NULL` (V5); nulo só em empréstimo concluído |
| item_id | BIGINT | FK |
| data_emprestimo | DATE | CU 10 passo 08 |
| data_prevista | DATE | calculada, não digitada |
| data_devolucao | DATE | nula até a devolução |
| status | VARCHAR(20) | EM_ANDAMENTO, ATRASADO, CONCLUIDO |
| dias_atraso | INTEGER | CU 12 fluxo 5.1 |
| registrado_por | BIGINT | FK `ON DELETE SET NULL`, Administrador que operou |

O schema atual (V4) não possui campos financeiros. O CU 12, fluxo 5.1 registra
os dias de atraso no empréstimo vinculado ao usuário. A V4 remove `valor_multa`
e `multa_paga` legados, descartando apenas esses dados financeiros e preservando
empréstimos, datas e dias de atraso. A V3 permanece intacta para o Flyway.

**Constraints**

- `chk_emprestimo_devolucao` — só empréstimo CONCLUIDO tem data de devolução.
- `chk_emprestimo_prazo` — data prevista nunca anterior ao empréstimo.
- `chk_emprestimo_atraso` — `dias_atraso >= 0`, sem condição financeira desde a V4.
- `uq_emprestimo_ativo` — índice parcial que impede o mesmo usuário ter dois
  empréstimos abertos do mesmo item.
- `chk_emprestimo_usuario_ativo` (V5) — empréstimo aberto sempre tem usuário;
  por isso excluir usuário com empréstimo aberto falha também no banco.

---

## reserva

Caso de uso 11.

| Coluna | Tipo | Observação |
| --- | --- | --- |
| id | BIGSERIAL | PK |
| usuario_id | BIGINT | FK `ON DELETE SET NULL` (V5); nulo só em reserva encerrada |
| item_id | BIGINT | FK |
| data_reserva | DATE | CU 11 passo 05 |
| validade_maxima | DATE | do protótipo |
| posicao_fila | INTEGER | CU 11 passo 06 |
| status | VARCHAR(20) | AGUARDANDO, DISPONIVEL, ATENDIDA, EXPIRADA, CANCELADA |
| registrado_por | BIGINT | FK `ON DELETE SET NULL` |

**Constraints**

- `uq_reserva_ativa` — índice parcial que implementa o fluxo 5.1 (reserva
  duplicada) no próprio banco, não só na aplicação.
- `chk_reserva_usuario_ativo` (V5) — reserva AGUARDANDO ou DISPONIVEL sempre
  tem usuário.

O status `DISPONIVEL` representa o momento do fluxo 7.1 do CU 12: o item foi
devolvido e o exemplar fica separado para o primeiro da fila até a validade.
Só esse usuário pode retirá-lo; ao retirar, a reserva passa a `ATENDIDA`.

## Exclusão de usuário e histórico (V5)

O CU 5, passo 04 remove o registro do usuário. A V5 troca as FKs de usuário
por `ON DELETE SET NULL`: empréstimos e reservas encerrados permanecem, sem o
vínculo, e as consultas os exibem como "(usuário excluído)". As constraints
`chk_*_usuario_ativo` fazem o `DELETE` falhar se houver movimentação ativa.

---

## Rastreabilidade

| Regra do documento | Onde é garantida |
| --- | --- |
| CU 3 — CPF ou matrícula obrigatório | `chk_usuario_identificacao` + `UsuarioService` |
| CU 4 — perfil do último/próprio Administrador | `UsuarioService.editar` |
| CU 5 fluxo 4.1 — exclusão bloqueada | `UsuarioService.excluir` + `chk_*_usuario_ativo` (V5) |
| CU 7 fluxo 5.1 — ISBN duplicado | `UNIQUE (isbn)` + `ItemService` |
| CU 10 fluxo 3.1 — item indisponível | `chk_item_quantidade_disp` + `decrementarDisponivel` |
| CU 10 passos 08 e 09 — registro e baixa atômicos | `TransacaoJdbc` em `EmprestimoService.registrar` |
| CU 10 fluxo 7.1 — limite de empréstimos | `EmprestimoService.registrar` |
| CU 10 passo 07 — pendência por atraso aberto | `EmprestimoDAO.possuiPendencia`: não concluído e prazo anterior a `CURRENT_DATE` |
| CU 11 fluxo 5.1 — reserva duplicada | `uq_reserva_ativa` + `ReservaService` |
| CU 12 fluxo 5.1 — cálculo de atraso | `Emprestimo.calcularDiasAtraso` |
| CU 12 fluxo 7.1 — prioridade do 1º da fila | `EmprestimoService.registrarDevolucao` + `registrar` |
| RNF-05 — criptografia de senhas | `util.Senhas` (BCrypt custo 12) |

Regras críticas aparecem duas vezes de propósito: na aplicação, para dar
mensagem clara ao operador, e no banco, como última linha de defesa.
