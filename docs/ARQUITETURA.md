# Arquitetura

## Visão geral

O BiblioTech é uma aplicação desktop de processo único. Não existe front-end
separado do back-end: view, service e dao vivem na mesma JVM e se comunicam
por chamada de método. A única fronteira externa é a conexão JDBC com o
PostgreSQL.

```
JavaFX (view) → service → dao → PostgreSQL
```

A regra de dependência é estrita. Cada camada só conhece a de baixo. Se um
controller importar `java.sql.Connection`, houve violação da arquitetura.

| Camada | Responsabilidade | Não pode |
| --- | --- | --- |
| `view` | Reagir a eventos, exibir e validar formato | Conter regra de negócio ou SQL |
| `service` | Regras dos casos de uso, orquestração, transações | Conhecer JavaFX ou SQL |
| `dao` | Traduzir objeto ↔ tabela | Conter regra de negócio |
| `model` | Estrutura de dados e cálculos próprios da entidade | Conhecer banco ou tela |

`model`, `exception` e `util` são transversais: qualquer camada pode usá-los.

---

## Injeção de dependências manual

Não há Spring nem container. Todas as instâncias são criadas uma única vez em
`App.init()` e passadas por construtor:

```java
var usuarioDAO   = new UsuarioDAOPostgres(dataSource);
var usuarioSvc   = new UsuarioService(usuarioDAO);
```

Isso deixa explícito quem depende de quem e — mais importante — permite
substituir o DAO por um mock nos testes, já que a dependência entra pelo
construtor e o tipo declarado é a interface.

---

## Threading: a armadilha do JavaFX

O JavaFX tem uma única thread encarregada de desenhar a interface, a
*JavaFX Application Thread*. Qualquer trabalho demorado executado nela
congela a janela.

Duas operações do BiblioTech são lentas o bastante para isso importar:

- a verificação BCrypt no login, que leva cerca de 250 ms por projeto;
- as consultas de relatório do CU 13 sobre períodos longos.

**Regra adotada:** toda chamada a `service` acontece dentro de um
`javafx.concurrent.Task`, e toda atualização de componente acontece em
`setOnSucceeded`.

```java
Task<List<Item>> tarefa = new Task<>() {
    @Override protected List<Item> call() {
        return App.servicos().itens().pesquisar(termo);   // thread de fundo
    }
};
tarefa.setOnSucceeded(e -> tabela.getItems().setAll(tarefa.getValue()));  // thread da UI
tarefa.setOnFailed(e -> Alertas.erro("Falha na pesquisa.", tarefa.getException()));

Thread t = new Thread(tarefa);
t.setDaemon(true);   // não impede o encerramento da aplicação
t.start();
```

`LoginController` e `VisaoGeralController` servem de referência para os demais.

Como várias tarefas podem rodar em paralelo, o pool do HikariCP é configurado
com 5 conexões, e não 1.

---

## Sessão

Em uma aplicação web o back-end é stateless e a sessão vive num cookie ou
token. Aqui, depois do login o objeto `Usuario` fica em `util.Sessao`, um
singleton em memória, até sair da sessão ou encerrar o processo. É mais simples e
atende ao CU 1.

---

## Tratamento de erros

A hierarquia de exceções espelha a especificação:

```
RuntimeException
├── DataAccessException          falha técnica (SQLException encapsulada)
└── RegraNegocioException        violação de regra do documento
    ├── CredenciaisInvalidasException     CU 1, fluxo 3.1
    ├── DadosDuplicadosException          CU 3/4 fluxo 4.1, CU 7 fluxo 5.1
    ├── ExclusaoNaoPermitidaException     CU 5, fluxo 4.1
    ├── UsuarioNaoEncontradoException     CU 9, fluxo 4.1
    ├── ItemIndisponivelException         CU 10, fluxo 3.1
    ├── LimiteExcedidoException           CU 10, fluxo 7.1
    └── PendenciaException                CU 10, fluxo 7.1
```

Na view a distinção define a apresentação: `RegraNegocioException` vira
mensagem inline ou diálogo explicativo, porque o usuário pode corrigir;
`DataAccessException` vira diálogo de erro, porque não pode.

Todas são `RuntimeException` por decisão consciente — exceções verificadas
obrigariam `throws` em toda a cadeia sem nenhum ganho real, já que a view
não tem como tratar uma `SQLException` de forma diferente.

---

## Persistência

**JDBC com DAO, sem ORM.** A escolha é deliberada: os diagramas de sequência
entregues mostram `Controller → Service → DAO → Banco`, e com Hibernate a
implementação deixaria de corresponder ao artefato documentado. Além disso,
o SQL visível é mais fácil de defender numa apresentação.

Práticas obrigatórias em todo DAO:

- `try-with-resources` em `Connection`, `PreparedStatement` e `ResultSet`;
- `PreparedStatement` com `?` — nunca concatenação de string;
- um método `mapear(ResultSet)` privado por entidade;
- `SQLException` encapsulada em `DataAccessException`.

Os JOINs de `EmprestimoDAOPostgres` e `ReservaDAOPostgres` trazem usuário e
item na mesma consulta, evitando o problema N+1 ao montar as tabelas.

---

## Concorrência no acervo

**Transações.** `util.Transacao` delimita a unidade de trabalho sem expor JDBC
ao service; `dao.TransacaoJdbc` a implementa e também é o `DataSource` entregue
aos DAOs em `App.init()`. Dentro de `executar`, todo `getConnection()` da mesma
thread recebe a conexão da transação, e o `close()` do try-with-resources do DAO
não a devolve ao pool antes do commit ou rollback. Assim os DAOs seguem o padrão
de sempre. O CU 10 (passos 07 a 09 e baixa da reserva atendida) e o CU 12
(passos 06 e 07 e separação do exemplar para o 1º da fila) confirmam ou desfazem
o movimento inteiro. Nos testes unitários, `Transacao.direta()` só executa a ação.

A geração MAX+1 de tombo, código e posição na fila ainda pode colidir entre dois
atendimentos simultâneos. Ver `PENDENCIAS.md`.

`decrementarDisponivel` usa `WHERE id = ? AND quantidade_disp > 0` e devolve
`false` quando não afeta nenhuma linha. Isso torna a operação atômica: se
dois atendimentos disputarem o último exemplar, apenas um vence, e o outro
recebe `ItemIndisponivelException`. A constraint
`chk_item_quantidade_disp` garante o mesmo invariante no banco.

---

## Estratégia de testes

| Camada | Ferramenta | Volume | O que valida |
| --- | --- | --- | --- |
| `service` | JUnit 5 + Mockito | Muitos | Regras e fluxos alternativos |
| `dao` | JUnit 5 + Testcontainers | Poucos | SQL, mapeamento e constraints |
| `view` | — | Nenhum | Custo maior que o benefício no prazo |

Os dois níveis são complementares. Mockito é rápido e aponta o defeito com
precisão, mas jamais pega um nome de coluna errado — só o teste contra banco
real pega. Testcontainers sobe um PostgreSQL efêmero, roda as mesmas
migrations e destrói o container ao final, o que evita banco compartilhado
entre os integrantes.

Testes de integração levam a tag `@Tag("integracao")` e ficam fora da suíte
padrão, para que `mvn test` continue rodando em segundos.

## Navegação e recursos visuais

A visão geral abre `Navegador.Tela.RELATORIO`. A primeira aba, **Gerar
Relatório**, segue o CU 13: tipo (passo 01), filtros do tipo (passo 02) e
relatório com cabeçalho e resumo (passo 04), com um método de `RelatorioService`
por tipo. As consultas de empréstimos e reservas são as outras abas de
`relatorio.fxml`, controladas por `RelatorioController`, sem rotas independentes
ou atalhos na tela inicial.
Cada aba captura seus filtros antes de iniciar um `Task`, chama o service
correspondente e atualiza a tabela em `setOnSucceeded`. Durante a consulta,
os filtros ficam desabilitados para impedir respostas concorrentes na mesma aba.

Os livros e ícones exportados do Figma ficam em `resources/images`. A fonte
Montserrat e sua licença ficam em `resources/fonts`; seu carregamento é local.
As classes CSS `tela-prototipo`, `login` e `inicial` delimitam os estilos dos
frames de referência. O fundo usa gradiente JavaFX e não exige recursos remotos.
O Figma é utilizado apenas durante o desenvolvimento, nunca durante a execução.

O menu no ícone de perfil contém somente **Administrar usuários**. Cadastrar item,
realizar reserva e realizar devolução ficam no acesso rápido junto de Gerar Relatório
e Pesquisar Acervo. Um `FlowPane` distribui os cinco botões conforme a largura disponível.
Enquanto um FXML ainda não existir, `Navegador` exibe aviso via `Alertas` e conserva
a tela atual. O inventário dessas pendências está em `PENDENCIAS.md`.

## Controle de atraso sem cobrança

`Emprestimo` calcula dias de atraso; `EmprestimoService.registrarDevolucao`
registra esse total e conclui a devolução. Não existem cálculo ou estado de multa.
`EmprestimoDAO.possuiPendencia` consulta empréstimos não concluídos com prazo
anterior à data atual do PostgreSQL, sem depender da atualização de status no start.
O service usa esse resultado para bloquear um novo empréstimo por atraso aberto.

O Flyway aplica V4 na inicialização, removendo apenas os campos financeiros legados
e mantendo os demais dados. A integração verifica tanto instalação limpa quanto
migração V3 → V4, além do rollback da transação e da exclusão com histórico (V5).
