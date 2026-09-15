# Pendências do esqueleto

O que está pronto e o que falta, para o grupo dividir as tarefas.

## Pronto

- `pom.xml` com toda a stack e versões travadas
- `docker-compose.yml` do PostgreSQL 17
- Migrations V1 a V4; V4 remove multas e preserva dias de atraso e histórico
- Seed parcial de usuários/itens (desativado por padrão; movimentações coerentes ainda pendentes)
- Todos os models e enums
- Todas as interfaces DAO e as 4 implementações PostgreSQL
- Base dos services com regras e fluxos alternativos; consistência transacional,
  atendimento da fila, edição de perfil e relatórios ainda precisam de complementos
- 9 exceções mapeadas aos fluxos do documento
- Utilitários: configuração, pool, sessão, senhas, datas
- `App.java` com bootstrap do Flyway e do administrador inicial
- `Navegador` e `Alertas`
- CSS completo com a identidade do protótipo
- Tela de login (FXML + controller) — **referência de padrão**
- Tela de visão geral (FXML + controller) — **referência de padrão**
- Login e inicial ajustados ao Figma, com imagens e Montserrat locais
- Relatórios com abas de consulta de empréstimos e reservas, filtros e carregamento assíncrono
- 4 classes de teste unitário (34 casos) e 2 de integração (5 casos)
- Remoção de multa do domínio, persistência, configuração e bloqueios
- Seleção de integração corrigida e execução comprovada com Docker
- README e documentação em `docs/`

## Falta implementar

Cada item abaixo é um FXML mais um controller. Use `login.fxml` /
`LoginController` e `visao-geral.fxml` / `VisaoGeralController` como modelo:
o padrão de `Task` para chamadas ao service já está demonstrado nos dois.

| Tela | Arquivo FXML | Controller | Caso de uso |
| --- | --- | --- | --- |
| Administrar usuários | `usuarios.fxml` | `UsuariosController` | 2, 4, 5, 6 |
| Cadastrar usuário | `cadastro-usuario.fxml` | `CadastroUsuarioController` | 3 |
| Pesquisar acervo | `acervo.fxml` | `AcervoController` | 8 |
| Cadastrar item | `cadastro-item.fxml` | `CadastroItemController` | 7 |
| Realizar empréstimo | `emprestimo.fxml` | `EmprestimoController` | 10 (+9) |
| Realizar reserva | `reserva.fxml` | `ReservaController` | 11 (+9) |
| Realizar devolução | `devolucao.fxml` | `DevolucaoController` | 12 (+9) |

Os nomes já estão registrados no enum `Navegador.Tela`, então basta criar os
arquivos com esses caminhos exatos.

## Sugestão de divisão para 5 pessoas

1. Administrar usuários + cadastro de usuário (CU 2 a 6)
2. Acervo: pesquisa + cadastro de item (CU 7 e 8)
3. Empréstimo e devolução (CU 10 e 12) — a parte mais densa
4. Registro de reserva (CU 11)
5. Completar os tipos de relatório do CU 13 e implementar exportação CSV

O trabalho restante inclui interface e ligação, mas também correções no backend.
A revisão encontrou gravações separadas de empréstimo/estoque e devolução/estoque,
prioridade da reserva não integrada à retirada, geração de identificadores via
MAX+1, edição de perfil incompleta e conflito entre exclusão e histórico.
Os testes unitários existentes não comprovam esses cenários com PostgreSQL.
Também falta completar os dados de demonstração: o seed reduz a disponibilidade
de itens sem criar os empréstimos correspondentes.

## Relatórios: entrega atual e trabalho restante

As consultas estão implementadas nas duas abas de `relatorio.fxml`, acessíveis
somente por **Gerar Relatório** na visão geral. Não criar telas ou atalhos
independentes de consulta de empréstimos e reservas.

Ainda falta completar o CU 13: exportação CSV, relatório de itens reservados
**e disponíveis**, histórico por usuário e prévia de resumo por período.
`RelatorioService.gerar(ITENS_RESERVADOS, ...)` ainda retorna lista vazia;
as consultas entregues usam os métodos `pesquisar` dos services existentes.
Os demais destinos de navegação ainda sem FXML exibem um aviso e mantêm a tela atual.

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
