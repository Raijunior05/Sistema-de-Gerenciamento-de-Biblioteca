# AGENTS.md

Instruções para agentes de IA que trabalham neste repositório.
Leia este arquivo por inteiro antes da primeira alteração.

---

## 1. O que é este projeto

**BiblioTech** é um sistema de gerenciamento de bibliotecas, entregue como
**aplicação desktop Java**. É o trabalho prático da disciplina Engenharia de
Software II (UNIVASF, 2026.2, Prof. Dr. Ricardo Argenton Ramos), Grupo 1.

O contexto acadêmico importa para as decisões técnicas: a nota depende de a
implementação **corresponder aos artefatos UML entregues**. Código elegante
que contradiz o documento de requisitos vale menos que código simples que o
reflete fielmente. Quando houver conflito entre "melhor prática" e "o que o
documento diz", siga o documento e registre a divergência.

### Restrições não negociáveis

| Restrição | Origem | Consequência prática |
| --- | --- | --- |
| Java como linguagem | Enunciado da disciplina | Não propor Kotlin, Python, etc. |
| SGBD relacional (MySQL ou PostgreSQL) | Enunciado | Não usar SQLite, MongoDB, H2 em produção |
| Aplicação desktop, sem internet | RNF-02 | Nada de API externa, CDN, serviço em nuvem |
| Execução local | RNF-02 | Docker é permitido (roda local), nuvem não |
| Senhas criptografadas | RNF-05 | BCrypt obrigatório; nunca texto puro ou SHA simples |

---

## 2. Stack

| Camada | Tecnologia | Versão | Não substituir por |
| --- | --- | --- | --- |
| Linguagem | Java (Temurin) | 21 LTS | — |
| Interface | JavaFX + FXML | 21.0.4 | Swing, Web |
| Build | Maven | 3.9+ | Gradle |
| Banco | PostgreSQL | 17 | — |
| Pool | HikariCP | 5.1.0 | — |
| Migrations | Flyway | 10.20.1 | Liquibase |
| Hash | BCrypt (at.favre.lib) | 0.10.2 | SHA-256, MD5 |
| Testes | JUnit 5 + Mockito | 5.11 / 5.14 | JUnit 4, TestNG |
| Integração | Testcontainers | 1.20.4 | banco compartilhado |

**Não adicione dependências novas sem necessidade clara.** Em especial, não
introduza Hibernate/JPA nem Spring: a escolha por JDBC puro é deliberada
(ver seção 5).

Mantenha `slf4j-api` e `slf4j-simple` na mesma versão (2.0.16): a API transitiva
antiga impede o carregamento da implementação de log atual.

---

## 3. Comandos

```bash
# Subir o banco
docker compose up -d
docker compose ps                 # conferir healthcheck
docker compose down -v            # zerar o banco (apaga os dados)

# Rodar a aplicação
mvn clean javafx:run

# Compilar sem rodar
mvn clean compile

# Testes unitários (rápidos, sem banco) — use este por padrão
mvn test

# Testes de integração (exigem Docker rodando)
mvn test -Dgroups=integracao -DgruposExcluidos=
# Docker 29 / Testcontainers 1.20.4: acrescente -Dapi.version=1.44

# Rodar uma classe de teste específica
mvn test -Dtest=EmprestimoServiceTest

# Empacotar
mvn clean package

# Dados de demonstração (banco no ar e schema já criado pelo app)
powershell -ExecutionPolicy Bypass -File scripts\seed-demo.ps1   # ou: sh scripts/seed-demo.sh
```

O seed usa e-mails `@demo.bibliotech.local` e tombos/códigos `DEM-` para poder
ser reexecutado sem afetar outros dados. Ao mudar o schema, confira se
`scripts/seed-demo.sql` continua válido; não o transforme em migration.

Credenciais do administrador criado na primeira execução:
`admin@bibliotech.local` / `admin123`.

---

## 4. Estrutura

```
bibliotech/
├── AGENTS.md                  este arquivo
├── README.md                  instruções para humanos
├── pom.xml
├── docker-compose.yml
├── docs/
│   ├── ARQUITETURA.md         camadas, threading, decisões
│   ├── MODELO-DADOS.md        tabelas, constraints, rastreabilidade
│   └── PENDENCIAS.md          o que falta implementar
├── scripts/
│   └── seed-demo.sql/.ps1/.sh dados de demonstração (execução manual)
└── src/
    ├── main/
    │   ├── java/br/univasf/bibliotech/
    │   │   ├── App.java            entrada; monta dependências, roda Flyway
    │   │   ├── model/              entidades e enums
    │   │   ├── view/               controllers JavaFX, Navegador, Alertas
    │   │   ├── service/            regras de negócio
    │   │   ├── dao/                interfaces + implementações PostgreSQL
    │   │   ├── exception/          uma por fluxo alternativo
    │   │   └── util/               config, pool, sessão, senhas, datas
    │   └── resources/
    │       ├── database.properties
    │       ├── css/sgb.css
    │       ├── fxml/
    │       └── db/migration/
    └── test/java/br/univasf/bibliotech/
        ├── service/                testes com Mockito
        └── dao/                    testes com Testcontainers
```

---

## 5. Arquitetura — regras que não podem ser quebradas

### Direção das dependências

```
view → service → dao → PostgreSQL
```

Cada camada só conhece a de baixo. `model`, `exception` e `util` são
transversais e podem ser usados por todos.

**Violações que devem ser recusadas:**

- controller (`view`) importando `java.sql.*` ou chamando DAO diretamente
- `service` importando `javafx.*`
- `dao` contendo regra de negócio (limite de empréstimo, cálculo de prazo)
- `model` conhecendo banco ou tela

### Por que JDBC e não ORM

Os diagramas de sequência entregues ao professor mostram
`Controller → Service → DAO → Banco`. Com Hibernate, a implementação
deixaria de corresponder ao artefato documentado. Mantenha o SQL visível.

### Injeção de dependência é manual

Tudo é instanciado uma única vez em `App.init()` e passado por construtor.
O tipo declarado é sempre a **interface** do DAO, nunca a implementação —
é isso que permite trocar por mock nos testes.

```java
var usuarioDAO = new UsuarioDAOPostgres(dataSource);   // implementação
UsuarioService svc = new UsuarioService(usuarioDAO);   // recebe a interface
```

### Threading no JavaFX — erro mais comum

O JavaFX tem **uma única thread** que desenha a interface. Chamar o banco
nela congela a janela. A verificação BCrypt leva ~250 ms; relatórios levam
mais.

**Regra obrigatória:** toda chamada a `service` dentro de um `Task`,
toda atualização de componente em `setOnSucceeded`.

```java
Task<List<Item>> tarefa = new Task<>() {
    @Override protected List<Item> call() {
        return App.servicos().itens().pesquisar(termo);   // thread de fundo
    }
};
tarefa.setOnSucceeded(e -> tabela.getItems().setAll(tarefa.getValue()));
tarefa.setOnFailed(e -> Alertas.erro("Falha na pesquisa.", tarefa.getException()));

Thread t = new Thread(tarefa, "pesquisa");
t.setDaemon(true);
t.start();
```

Use `LoginController` e `VisaoGeralController` como referência — os dois já
seguem o padrão.

---

## 6. Domínio: os 13 casos de uso

Dois atores apenas. **Administrador** é o único que faz login e opera o
sistema. **Usuário** (o leitor) tem cadastro e participa das movimentações,
mas não acessa a aplicação. O ator Bibliotecário foi eliminado na versão 3 do
documento — não reintroduza.

| # | Caso de uso | Service | Observação |
| --- | --- | --- | --- |
| 1 | Validar Usuário | `AutenticacaoService` | Login do Administrador |
| 2 | Administrar Usuários | — | Tela agregadora; ponto de extensão |
| 3 | Cadastrar Usuário | `UsuarioService.cadastrar` | «extend» do 2 |
| 4 | Editar Usuário | `UsuarioService.editar` | «extend» do 2 |
| 5 | Excluir Usuário | `UsuarioService.excluir` | «extend» do 2 |
| 6 | Consultar Usuário | `UsuarioService.pesquisar` | «extend» do 2 |
| 7 | Cadastrar Item | `ItemService.cadastrar` | Gera tombo |
| 8 | Pesquisar Acervo | `ItemService.pesquisar` | |
| 9 | Identificar Usuário | `UsuarioService.identificar` | «include» de 10, 11 e 12 |
| 10 | Realizar Empréstimo | `EmprestimoService.registrar` | |
| 11 | Realizar Reserva | `ReservaService.registrar` | «extend» do 10 |
| 12 | Realizar Devolução | `EmprestimoService.registrarDevolucao` | |
| 13 | Gerar Relatório | `RelatorioService` | |

### Regras de negócio implementadas

| Regra | Fluxo | Onde | Exceção |
| --- | --- | --- | --- |
| Credenciais inválidas | CU 1, 3.1 | `AutenticacaoService` | `CredenciaisInvalidasException` |
| E-mail/CPF/matrícula duplicado | CU 3, 4.1 | `UsuarioService` | `DadosDuplicadosException` |
| CPF ou matrícula obrigatório | CU 3 | `UsuarioService` + constraint | `RegraNegocioException` |
| Exclusão com pendência | CU 5, 4.1 | `UsuarioService` | `ExclusaoNaoPermitidaException` |
| Último administrador | CU 5, 4.1 | `UsuarioService` | `ExclusaoNaoPermitidaException` |
| Nome de usuário do Administrador | CU 3, 4.1 | `UsuarioService` | `RegraNegocioException` |
| Rebaixar último/próprio Administrador | CU 4, 4.1 | `UsuarioService.editar` | `RegraNegocioException` |
| ISBN duplicado | CU 7, 5.1 | `ItemService` | `DadosDuplicadosException` |
| Usuário não encontrado | CU 9, 4.1 | `UsuarioService` | `UsuarioNaoEncontradoException` |
| Item indisponível | CU 10, 3.1 | `EmprestimoService` | `ItemIndisponivelException` |
| Exemplar separado para o 1º da fila | CU 12, 7.1 | `EmprestimoService.registrar` | `ItemIndisponivelException` |
| Limite de empréstimos | CU 10, 7.1 | `EmprestimoService` | `LimiteExcedidoException` |
| Empréstimo aberto vencido pela data atual | CU 10, 7.1 | `EmprestimoService` + `EmprestimoDAO.possuiPendencia` | `PendenciaException` |
| Reserva de item disponível | CU 11, 3.1 | `ReservaService` | `RegraNegocioException` |
| Reserva duplicada | CU 11, 5.1 | `ReservaService` + índice | `RegraNegocioException` |
| Cálculo de dias de atraso | CU 12, 5.1 | `Emprestimo.calcularDiasAtraso` | — |
| Prioridade do 1º da fila (exemplar separado, reserva DISPONIVEL) | CU 12, 7.1 | `EmprestimoService` | — |
| Registro + baixa / devolução + reposição atômicos | CU 10, 08–09; CU 12, 06–07 | `Transacao` | — |

**Não há multa no escopo atual.** O CU 12, fluxo 5.1 registra somente dias de atraso.
Não crie cobrança, quitação, `MultaService` ou bloqueio financeiro. Atraso histórico
de empréstimo concluído não bloqueia novo empréstimo. Veja a seção 17.

### Parâmetros configuráveis

Em `database.properties`, nunca em constante no código:

```properties
regra.limiteEmprestimosPorUsuario=3
regra.diasPrazoEmprestimo=15
regra.diasValidadeReserva=7
```

---

## 7. Banco de dados

Quatro tabelas: `usuario`, `item`, `emprestimo`, `reserva`.
Detalhamento completo em `docs/MODELO-DADOS.md`.

Migrations V1 a V5. A V5 troca as FKs de usuário por `ON DELETE SET NULL`
(histórico encerrado sobrevive à exclusão, exibido como "(usuário excluído)")
e acrescenta `chk_emprestimo_usuario_ativo` e `chk_reserva_usuario_ativo`.
Por isso os DAOs de empréstimo e reserva usam `LEFT JOIN usuario`.

### Transações

Services recebem `util.Transacao` (sem `java.sql`) e envolvem os movimentos em
`transacao.executar(...)`. A implementação `dao.TransacaoJdbc` também é o
`DataSource` passado aos DAOs em `App.init()`; não crie DAO com o pool direto
na aplicação. Nos testes unitários, use `Transacao.direta()`.

### Regra crítica sobre migrations

**Nunca edite uma migration já aplicada.** O Flyway guarda um checksum e vai
falhar o start. Para corrigir algo, crie uma migration nova com o próximo
número.

Nomenclatura obrigatória: `V{n}__{descricao_em_snake_case}.sql`
(dois underscores).

### Constraints são parte do design

Regras críticas aparecem duas vezes de propósito: no service, para dar
mensagem clara ao operador, e no banco, como última linha de defesa. Não
remova constraint achando que é redundante.

- `chk_usuario_identificacao` — exige CPF ou matrícula
- `chk_usuario_credenciais` — Administrador tem senha obrigatoriamente
- `chk_item_quantidade_disp` — `0 <= disponível <= total`
- `chk_emprestimo_devolucao` — só CONCLUIDO tem data de devolução
- `uq_emprestimo_ativo` — índice parcial, um empréstimo aberto por par
- `uq_reserva_ativa` — índice parcial, uma reserva ativa por par

### Concorrência

`decrementarDisponivel` usa `WHERE id = ? AND quantidade_disp > 0` e retorna
`false` quando nenhuma linha foi afetada. Isso torna a operação atômica.
Não substitua por `SELECT` seguido de `UPDATE`.

---

## 8. Convenções de código

### Idioma

- **Código, nomes de classe, método e variável: português sem acento.**
  `UsuarioService`, `calcularDiasAtraso`, `quantidadeDisponivel`.
- Javadoc e comentários: português, sem acento nos arquivos `.java`
  (evita problema de encoding entre as máquinas do grupo).
- Strings de interface (mensagens ao operador): português **com** acento.
- Markdown e SQL: português com acento normalmente.

### Estilo

- Indentação 4 espaços, sem tab.
- Chaves sempre, mesmo em `if` de uma linha.
- Linhas até ~110 caracteres.
- `final` em campos que não mudam.
- Imports explícitos, nunca `import java.util.*`.
- `var` apenas quando o tipo é óbvio pelo lado direito.

### Javadoc

Toda classe de `service` e `dao` e todo método público de service recebe
Javadoc **citando o caso de uso e o passo** que implementa:

```java
/**
 * CU 10 - Realizar Emprestimo, fluxo principal.
 *
 * @throws LimiteExcedidoException fluxo alternativo 7.1
 */
```

Isso é rastreabilidade para a banca, não decoração. Mantenha.

### DAO — padrão obrigatório

```java
@Override
public Optional<Usuario> buscarPorId(long id) {
    String sql = "SELECT ... FROM usuario WHERE id = ?";
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setLong(1, id);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
        }
    } catch (SQLException e) {
        throw new DataAccessException("Falha ao buscar usuario.", e);
    }
}
```

Não negociável:

- `try-with-resources` em `Connection`, `PreparedStatement` e `ResultSet`
- `PreparedStatement` com `?` — **nunca** concatenação de string em SQL
- um método `mapear(ResultSet)` por entidade
- `SQLException` sempre encapsulada em `DataAccessException`
- retorno `Optional` para busca de um, `List` vazia para busca de vários
  (nunca `null`)

### Exceções

Hierarquia em `exception/`. Todas são `RuntimeException`, por decisão
consciente. `DataAccessException` é falha técnica; `RegraNegocioException` e
suas subclasses são violações do documento. A view usa essa distinção: regra
de negócio vira mensagem inline ou diálogo explicativo, falha técnica vira
diálogo de erro.

---

## 9. Testes

### Estratégia

| Camada | Ferramenta | Volume | Valida |
| --- | --- | --- | --- |
| `service` | JUnit 5 + Mockito | Muitos | Regras e fluxos alternativos |
| `dao` | JUnit 5 + Testcontainers | Poucos | SQL, mapeamento, constraints |
| `view` | — | Nenhum | Fora de escopo no prazo |

Os dois níveis são complementares. Mockito é rápido e localiza o defeito com
precisão, mas **nunca pega nome de coluna errado**. Só o teste contra banco
real pega.

### Convenção de nomes

Um teste por fluxo da especificação, com nome descritivo:

```java
@Nested
@DisplayName("CU 10 - Realizar Emprestimo")
class RealizarEmprestimo {

    @Test
    @DisplayName("fluxo 7.1: limite de emprestimos atingido")
    void deveRecusarQuandoLimiteAtingido() { ... }
}
```

O relatório de testes vira evidência de que o sistema implementa o
documentado. É o argumento mais forte da apresentação — mantenha os
`@DisplayName` referenciando os fluxos.

### Ao adicionar regra nova

Toda regra de negócio nova precisa de teste antes de ser considerada pronta.
Verifique o caminho feliz **e** o de exceção, e use `verify(dao, never())`
para provar que nada foi gravado quando a regra bloqueia.

### Testes de integração

Levam `@Tag("integracao")` e ficam fora da suíte padrão (configurado no
`maven-surefire-plugin`). Exigem Docker. Não os coloque na suíte padrão —
`mvn test` precisa continuar rodando em segundos.

---

## 10. Interface

### Identidade visual

A paleta vem do protótipo Figma (`SGB.svg`) e está em `css/sgb.css`.
**Não invente cores.**

| Token | Hex | Uso |
| --- | --- | --- |
| navy-900 | `#0B1B3D` | fundo |
| navy-700 | `#2D3748` | painéis e cartões |
| navy-600 | `#4A5568` | campos de entrada |
| gold | `#C5A059` | títulos, rótulos, destaque |
| claro | `#F0F4F8` | texto e botão primário |
| verde | `#2E7D32` | status ativo |
| vermelho | `#C62828` | status atrasado |

Login e visão geral usam painéis translúcidos com raio de 23px, campos arredondados
e botões com raio de 17px na escala de 960 × 540, conforme os frames do Figma.
Use Montserrat e os recursos locais em `resources/fonts` e `resources/images`.
As demais telas podem manter os estilos existentes com raio de 2px.

### Padrão de tela

- Estilize pelas classes existentes no CSS (`painel`, `cartao`, `botao-primario`,
  `titulo-tela`, `rotulo-campo`, `badge-ativo`...). Adicione classe nova ao
  `sgb.css` em vez de usar `style=` inline.
- Registre a tela no enum `Navegador.Tela` e navegue com
  `Navegador.irPara(...)`.
- Diálogos sempre via `Alertas`, nunca `new Alert(...)` solto.
- Validação de formato (campo vazio, e-mail malformado) pode ficar no
  controller. Validação de **regra** fica no service, sempre.

### Telas pendentes

Ver `docs/PENDENCIAS.md`. Os caminhos dos FXML já estão registrados no
`Navegador.Tela`; basta criar os arquivos com os nomes exatos.

---

## 11. Protótipo x especificação

O protótipo Figma tem oito telas e diverge do documento em pontos
importantes. **O documento vence.** Orientações para a implementação:

1. "Pesquisar Acervo" deve exibir colunas de item; tela pendente
2. "Cadastro Leitor" deve incluir nome, CPF/matrícula e perfil; tela pendente
3. Senha era pedida a todo leitor — agora só para Administrador
4. "Cadastro Empréstimo" deve usar CPF/matrícula, item do acervo e prazo calculado; tela pendente
5. Rótulos diziam "Usuário" onde é o Administrador que opera — corrigidos
6. Login por e-mail x nome de usuário — aceita os dois
7. Falta tela de registro de reserva — especificada, implementação pendente
8. Exportação PDF/Excel — CSV planejado, implementação pendente

Antes de implementar uma tela, confira os requisitos e as orientações acima.

---

## 12. O que não fazer

- Não reintroduzir o ator **Bibliotecário** (removido na versão 3)
- Não reintroduzir multas; o CU 12, fluxo 5.1 calcula somente dias de atraso
- Não adicionar Hibernate, JPA, Spring ou Lombok
- Não usar API externa, CDN ou qualquer serviço de rede (RNF-02)
- Não editar migration já aplicada — crie a próxima
- Não chamar service fora de `Task` num controller
- Não concatenar string em SQL
- Não guardar senha em texto puro nem usar SHA/MD5
- Não colocar a aplicação JavaFX dentro de container Docker (só o banco)
- Não commitar `database.properties` com senha real de produção
- Não remover os `@DisplayName` que referenciam fluxos da especificação
- Não renomear classes de forma que desalinhe dos diagramas UML entregues

---

## 13. Ponto em aberto

O CU 1 se chama **"Validar Usuário"** no documento. Como agora ele trata só
do login do Administrador e existe um ator chamado Usuário, o nome é ambíguo.
Sugestão registrada com o grupo: renomear para "Autenticar Administrador".
**Enquanto não houver decisão, mantenha o nome do documento** em Javadoc e
`@DisplayName` — a implementação já usa `AutenticacaoService` como nome de
classe, o que é compatível com as duas opções.

---

## 14. Antes de terminar qualquer tarefa

1. `mvn clean compile` passa sem warning novo
2. `mvn test` passa inteiro
3. Regra nova tem teste cobrindo caminho feliz e exceção
4. Javadoc dos métodos públicos de service cita CU e passo
5. Nenhuma violação da direção de dependências da seção 5
6. Se mexeu no schema, criou migration nova (não editou existente)
7. Se mexeu em regra documentada, conferiu se `docs/` continua verdadeiro

## 15. Navegação e manutenção da documentação (15/09/2026)

- `relatorio.fxml` tem a aba **Gerar Relatório** (CU 13: tipo, filtros do tipo,
  relatório com resumo) e as abas de consulta. Consultas de empréstimos e reservas
  existem **somente dentro de Relatórios**, em abas de `relatorio.fxml` / `RelatorioController`. Não recrie atalhos na inicial
  nem rotas `CONSULTA_EMPRESTIMOS` / `CONSULTA_RESERVAS`.
- Acesso rápido da visão geral: **Gerar Relatório**, **Pesquisar Acervo**,
  **Cadastrar item**, **Realizar reserva** e **Realizar devolução**.
  Use quebra de linha dos botões conforme a largura disponível.
- Rodapé: cadastro de leitor, cadastro de empréstimo, perfil do administrador
  com menu contendo somente **Administrar usuários** e sair. Preserve o nome real do administrador.
- Os indicadores continuam usando `RelatorioService` dentro de `Task`.
  As consultas reutilizam `EmprestimoService.pesquisar` e `ReservaService.pesquisar`
  também em `Task`; leia os filtros na thread JavaFX antes de iniciar a tarefa.
- Todas as telas dos casos de uso existem. A exportação CSV continua pendente;
  não documente planejamento como entrega.
- A marca exibida nas telas é **BiblioTech** (antes "S G B"); as classes CSS
  `marca-sgb` mantêm o nome antigo.
- Atualize **README.md**, **AGENTS.md** e os documentos afetados em **docs/** em cada tarefa.
- Configuração local em `/database.properties` e arquivos de IDE/build ficam fora do Git.
  Na instalação nativa, crie o banco com o usuário da aplicação como proprietário.
- Preserve UTF-8 **sem BOM** nos FXML e Java. Confira o carregamento dos FXML:
  compilar Java não detecta erros de XML, CSS ou recursos visuais ausentes.

## 16. Pendências e estado das regras

- Consulte `docs/PENDENCIAS.md` para o estado da implementação. Valide os
  critérios de aceite contra os requisitos e os artefatos UML antes de ampliar regras.
- Transações dos movimentos, prioridade da fila, proteção de perfil na edição,
  preservação do histórico na exclusão e os quatro tipos do CU 13 foram concluídos
  em 24/09/2026. Continuam pendentes a geração concorrente de códigos (MAX+1),
  a promoção da fila na expiração de reserva e a exportação CSV.
- Atualize `PENDENCIAS.md` ao concluir uma tarefa. Não altere cartões
  externos sem solicitação explícita do usuário.

## 17. Requisitos sem multa (15/09/2026)

- O PDF atual, página 10, CU 12 fluxo 5.1, determina cálculo de dias de atraso.
  As menções residuais a multas nas páginas 2 e 9 foram resolvidas pela instrução
  explícita do usuário de retirar multas. Não trate esses trechos como nova exigência.
- A migration V4 remove `valor_multa` e `multa_paga`, preserva o histórico e mantém
  `dias_atraso >= 0`. V1–V3 permanecem imutáveis; referências financeiras em V3
  e na preparação do teste de migração são exclusivamente legado.
- Pendência de empréstimo é registro não concluído com `data_prevista < CURRENT_DATE`.
  Não dependa apenas do status ATRASADO atualizado na inicialização.
- Remoção de multas e seleção de testes de integração concluídas. Estado em
  24/09/2026: 53 testes unitários e 7 de integração aprovados com PostgreSQL 17.
  Docker 29 exigiu `-Dapi.version=1.44` no Maven.
