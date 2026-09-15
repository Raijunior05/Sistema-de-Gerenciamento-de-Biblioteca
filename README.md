# BiblioTech

Sistema de gerenciamento de bibliotecas — aplicação desktop Java.

**Disciplina:** Engenharia de Software II — Prof. Dr. Ricardo Argenton Ramos
**Grupo 1 — 2026.2:** Raimundo Ferreira do Nascimento Junior, Felipe Vieira de Oliveira,
Henrique Oliveira Rodrigues, Guilherme Miller Gama Cardoso, Alinne de Souza Santos Castro

---

## Stack

| Camada | Tecnologia | Versão |
| --- | --- | --- |
| Linguagem | Java (Eclipse Temurin) | 21 LTS |
| Interface | JavaFX + FXML | 21.0.4 |
| Build | Maven | 3.9+ |
| Banco | PostgreSQL | 17 |
| Pool de conexões | HikariCP | 5.1.0 |
| Versionamento do schema | Flyway | 10.20.1 |
| Hash de senhas | BCrypt (at.favre.lib) | 0.10.2 |
| Testes | JUnit 5 + Mockito | 5.11 / 5.14 |
| Testes de integração | Testcontainers | 1.20.4 |

---

## Como executar

### Pré-requisitos

- JDK 21 (`java -version` deve mostrar 21)
- Maven 3.9 ou superior
- PostgreSQL 17 — via Docker (opção A) ou instalado nativamente (opção B)

### Opção A — banco em Docker (recomendado)

```bash
docker compose up -d
```

Aguarde alguns segundos até o healthcheck ficar verde (`docker compose ps`).

### Opção B — PostgreSQL nativo

Instale o PostgreSQL 17 e crie o banco e o usuário:

```sql
CREATE USER bibliotech WITH PASSWORD 'bibliotech';
CREATE DATABASE bibliotech OWNER bibliotech;
```

Se usar outra porta ou senha, copie `src/main/resources/database.properties`
para `database.properties` na pasta de execução e ajuste essa cópia local.
O arquivo externo tem preferência sobre o empacotado e não deve ser versionado.

### Rodar a aplicação

```bash
mvn clean javafx:run
```

Na primeira execução o Flyway cria todas as tabelas e a aplicação cadastra um
administrador inicial, imprimindo as credenciais no console:

```
Acesso: admin@bibliotech.local
Senha:  admin123
```

A tela **Administrar usuários**, incluindo a redefinição de senha, ainda está pendente nesta versão. Veja `docs/PENDENCIAS.md`.

### Rodar os testes

```bash
mvn test                      # testes unitários (rápidos, sem banco)
mvn test -Dgroups=integracao -DgruposExcluidos=  # integração (exige Docker)
# Docker 29 com Testcontainers 1.20.4: acrescente -Dapi.version=1.44
```

---

## Estrutura

```
src/main/java/br/univasf/bibliotech/
├── App.java          ponto de entrada; monta as dependências e roda o Flyway
├── model/            entidades e enums de domínio
├── view/             controllers JavaFX, Navegador e Alertas
├── service/          regras de negócio (uma classe por grupo de casos de uso)
├── dao/              acesso a dados — interface + implementação PostgreSQL
├── exception/        uma exceção por fluxo alternativo da especificação
└── util/             configuração, pool, sessão, senhas e datas

src/main/resources/
├── database.properties   conexão e parâmetros das regras de negócio
├── css/sgb.css           identidade visual extraída do protótipo Figma
├── fxml/                 login, visão geral e relatórios
├── images/               livros e ícones originais do Figma, usados offline
├── fonts/                Montserrat e licença SIL OFL
└── db/migration/         migrations do Flyway

src/test/java/            testes unitários (Mockito) e de integração (Testcontainers)
docs/                     arquitetura, modelo de dados e pendências
```

A regra de dependência é estrita: `view → service → dao → banco`. Nenhuma
camada conhece a de cima, e um controller nunca importa `java.sql`.

---

## Parâmetros das regras de negócio

Ficam no `database.properties` externo e podem ser ajustados sem recompilar;
reinicie a aplicação para carregar os novos valores:

| Chave | Padrão | Caso de uso |
| --- | --- | --- |
| `regra.limiteEmprestimosPorUsuario` | 3 | CU 10, fluxo 7.1 |
| `regra.diasPrazoEmprestimo` | 15 | CU 10, passo 08 |
| `regra.diasValidadeReserva` | 7 | CU 11, passo 05 |

---

## Dados de demonstração

Para a apresentação, ative o seed renomeando o arquivo:

```bash
mv src/main/resources/db/migration/afterMigrate__seed_demo.sql.disabled \
   src/main/resources/db/migration/afterMigrate__seed_demo.sql
```

Ele popula usuários e acervo de exemplo. O seed ainda não cria movimentações
correspondentes à disponibilidade reduzida dos itens; não o use como evidência
de consistência de empréstimos ou reservas. Veja `docs/PENDENCIAS.md`.

---

## Documentação complementar

- `docs/ARQUITETURA.md` — camadas, threading no JavaFX e decisões técnicas
- `docs/MODELO-DADOS.md` — tabelas, constraints e rastreabilidade
- [docs/PENDENCIAS.md](docs/PENDENCIAS.md) — funcionalidades entregues e trabalho restante

## Interface e navegação

- **Login:** painel translúcido arredondado, campos arredondados, marca SGB e
  imagem original dos livros, conforme o [Figma SGB](https://www.figma.com/design/7vOy8UHiIwO9NGVkXVkxvu/SGB?node-id=6-21).
  Aceita e-mail ou login; somente Administrador acessa o sistema.
- **Visão geral:** três indicadores reais e acesso rápido a **Gerar Relatório**,
  **Pesquisar Acervo**, **Cadastrar item**, **Realizar reserva** e **Realizar devolução**.
  Os botões se distribuem em linhas conforme a largura da janela. As consultas de empréstimos e
  reservas ficam exclusivamente em **Relatórios**.
- **Rodapé:** nome do administrador, Cadastrar Leitor, Cadastrar Empréstimo e
  sair. O menu do perfil contém somente **Administrar usuários**.
- **Relatórios:** abas **Consultar Empréstimos** (busca, status e período de
  retirada) e **Consultar Reservas** (busca, status e ordem de posição na fila).
  Use **Pesquisar** ou Enter na busca; **Limpar filtros** volta à consulta completa.
  Carregamento, ausência de resultados e falhas têm mensagens próprias.
- Login, visão geral e consultas em Relatórios estão implementados. Os demais
  destinos ainda exibem aviso de indisponibilidade; veja `docs/PENDENCIAS.md`.
  Exportação CSV e os demais relatórios do CU 13 continuam pendentes.

Os recursos visuais e a fonte Montserrat são distribuídos junto da aplicação:
nenhuma conexão ao Figma ou à internet é feita durante o uso. A janela inicia
com área de conteúdo de 960 × 540 e permite ampliação.

## Atrasos sem multa

O sistema registra dias de atraso, sem cálculo, cobrança ou quitação de multas.
Somente empréstimos ainda abertos com prazo vencido geram essa pendência;
devoluções atrasadas permanecem no histórico sem bloqueio financeiro.

Na próxima inicialização, o Flyway aplica a migration V4 ao banco configurado:
remove os valores e indicadores de pagamento legados, preservando empréstimos,
datas e dias de atraso. As migrations anteriores foram mantidas intactas.
Não é necessário recriar o banco. Remova `regra.valorMultaPorDia` de eventual
`database.properties` externo; a propriedade deixou de ser utilizada.

Validação: compilação Java 21, 34 testes unitários e 5 testes de integração
aprovados em PostgreSQL 17 temporário, incluindo instalação limpa e atualização V3 → V4.
Os três FXML também foram carregados e renderizados com CSS e recursos locais,
usando serviços simulados. As funcionalidades ainda pendentes estão em `docs/PENDENCIAS.md`.
