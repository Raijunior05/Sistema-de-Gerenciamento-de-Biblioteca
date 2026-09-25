# Pendências do esqueleto

O que está pronto e o que falta, para o grupo dividir as tarefas.

## Pronto

- `pom.xml` com toda a stack e versões travadas
- `docker-compose.yml` do PostgreSQL 17
- Migrations V1 a V5; V4 remove multas e preserva dias de atraso e histórico;
  V5 permite excluir usuário mantendo o histórico encerrado (CU 5)
- Seed de demonstração em `scripts/seed-demo.sql` (usuários, acervo, empréstimos e
  reservas coerentes com as regras), que substitui o seed parcial desativado
- Todos os models e enums
- Todas as interfaces DAO e as 4 implementações PostgreSQL
- Services com regras e fluxos alternativos, incluindo transação única no
  empréstimo (CU 10, passos 08 e 09) e na devolução (CU 12, passos 06 e 07),
  prioridade garantida do 1º da fila e proteção de perfil na edição (CU 4)
- 9 exceções mapeadas aos fluxos do documento
- Utilitários: configuração, pool, sessão, senhas, datas
- `App.java` com bootstrap do Flyway e do administrador inicial
- `Navegador` e `Alertas`
- CSS completo com a identidade do protótipo
- Tela de login (FXML + controller) — **referência de padrão**
- Tela de visão geral (FXML + controller) — **referência de padrão**
- Login e inicial ajustados ao Figma, com imagens e Montserrat locais
- Relatórios: aba **Gerar Relatório** com os 4 tipos do CU 13 e abas de consulta
  de empréstimos e reservas, filtros e carregamento assíncrono
- Telas de todos os casos de uso (FXML + controller)
- 7 classes de teste unitário (53 casos) e 2 de integração (7 casos)
- Remoção de multa do domínio, persistência, configuração e bloqueios
- Seleção de integração corrigida e execução comprovada com Docker
- README e documentação em `docs/`

## Falta implementar

- Exportação do relatório (CSV planejado).
- Geração de tombo, código de empréstimo e posição na fila por `MAX + 1`:
  a transação não impede colisão entre dois atendimentos simultâneos.
- Reserva `DISPONIVEL` que expira no start não promove o próximo da fila;
  a promoção acontece apenas na devolução seguinte.
- Obrigatoriedade de "ISBN ou identificador" (CU 7) sem definição do grupo;
  hoje o campo é opcional e único quando informado.
- Itens menores da revisão de 24/09/2026 ainda não tratados: rótulos
  "Leitor" (o ator é Usuário desde a v3), status "Ativo" em vez de
  "em andamento", rótulo "E-mail" no login (aceita também nome de usuário),
  validade/expiração de reserva não prevista no documento, `new Alert` fora de
  `Alertas` em Consultar Usuário e Acervo e consulta de reservas dentro da tela
  de reserva.

## Sugestão de divisão para 5 pessoas

1. Exportação CSV do CU 13
2. Geração concorrente de códigos (sequence ou lock)
3. Promoção da fila na expiração de reserva
4. Itens menores da revisão listados acima

## Relatórios: entrega atual e trabalho restante

`relatorio.fxml` tem três abas, acessíveis somente por **Gerar Relatório** na
visão geral:

- **Gerar Relatório** (CU 13): o Administrador escolhe um dos quatro tipos do
  passo 01 — itens emprestados, itens reservados e disponíveis, atrasos e
  histórico de empréstimos por usuário —, a tela exibe só os filtros daquele
  tipo (passo 02), com atalhos diário, semanal e mensal de período, e o
  relatório sai com cabeçalho, período, data de geração e resumo (passo 04).
  Sem registros, aplica o fluxo 4.1.
- **Consultar Empréstimos** e **Consultar Reservas**: consultas livres por
  busca, status e período ou posição na fila.

Não criar telas ou atalhos independentes de consulta. Falta a exportação CSV.

## Validação visual anterior

- Compilação com Temurin 21 e suíte unitária de 34 testes.
- Carregamento dos três FXML e CSS e capturas em memória com DAOs simulados.
- Verificação visual na área de conteúdo 960 × 540; dados reais e integração
  com PostgreSQL devem ser conferidos no ambiente de apresentação.

## Validação da remoção de multas — 15/09/2026

- `mvn clean compile test`: 34 testes unitários aprovados com Java 21.
- `mvn test -Dgroups=integracao -DgruposExcluidos= -Dapi.version=1.44`:
  5 testes aprovados em PostgreSQL 17 temporário (Docker 29).
- Instalação limpa até V4 e atualização V3 → V4 com valor antigo não pago:
  histórico preservado, colunas financeiras removidas, bloqueio por prazo
  vencido mesmo com status EM_ANDAMENTO, devolução e novo empréstimo permitidos,
  constraint de dias não negativos mantida.
- As sete telas operacionais, transações e demais tasks continuam pendentes.

## Revisão para publicação — 15/09/2026

- Instruções de PostgreSQL nativo corrigidas para criar o banco com o proprietário correto.
- Configuração externa, diretórios de IDE e build excluídos do versionamento.
- API e implementação de log SLF4J alinhadas em 2.0.16 para evitar logger desativado.
- Os três FXML foram carregados e renderizados com CSS e imagens locais,
  usando serviços simulados e sem acessar o banco da aplicação.
- Mantidas as pendências funcionais acima; aprovação dos testes não significa
  conclusão das telas ou garantias transacionais ainda não implementadas.

## Alinhamento com casos de uso e diagramas — 24/09/2026

- CU 3: nome de usuário obrigatório para Administrador; cadastro volta para
  Administrar Usuários (CU 3 estende o CU 2); opções CPF/Matrícula rotuladas.
- CU 4: não retira o perfil do único Administrador nem do Administrador logado;
  promover a Administrador exige senha.
- CU 5: V5 troca as FKs de usuário por `ON DELETE SET NULL`; o histórico
  encerrado permanece como "(usuário excluído)" e movimentação ativa continua
  bloqueando a exclusão também no banco.
- CU 7: ISBN em branco gravado como nulo; mensagem de sucesso com os dados do
  item (passo 08); opções ISBN/Outro sem efeito removidas.
- CU 9: identificação exibe nome, CPF/matrícula e e-mail (passo 05).
- CU 10: tela na ordem do diagrama — item, disponibilidade (passo 03),
  identificação, registro; reserva oferecida só com item indisponível (3.1);
  data de retirada somente leitura; registro e baixa em uma transação.
- CU 12: devolução, reposição e separação do exemplar para o 1º da fila em uma
  transação; esse exemplar só pode ser retirado por ele, e a reserva vira ATENDIDA.
- CU 13: quatro tipos de relatório com filtros por tipo.
- Título "S G B" substituído por "BiblioTech" em todas as telas.
- `mvn clean test`: 53 testes unitários aprovados. `mvn test -Dgroups=integracao
  -DgruposExcluidos= -Dapi.version=1.44`: 7 testes aprovados em PostgreSQL 17.
- As 13 telas foram abertas contra o banco local com captura de cada uma e
  simulação do empréstimo (item indisponível) e dos tipos de relatório.
