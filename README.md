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

## Dados de demonstração (seed)

O seed preenche o banco com um cenário pronto para apresentar todos os casos
de uso: usuários, acervo, empréstimos em dia, atrasados e devolvidos, e reservas
em fila. Ele **não roda sozinho**: não é migration do Flyway nem é usado pelos
testes. Você executa quando quiser, e pode repetir a execução.

| Arquivo | Função |
| --- | --- |
| `scripts/seed-demo.sql` | o script SQL com todos os dados |
| `scripts/seed-demo.ps1` | atalho para Windows (PowerShell) |
| `scripts/seed-demo.sh` | atalho para Linux/macOS |

### Passo a passo (banco em Docker)

1. **Suba o banco** e espere o status `healthy`:

   ```bash
   docker compose up -d
   docker compose ps
   ```

2. **Abra a aplicação uma vez** (`mvn clean javafx:run`) e feche-a. É nesse
   momento que o Flyway cria as tabelas; sem elas o seed é interrompido com a
   mensagem *"Schema inexistente"*.

3. **Rode o seed** na raiz do projeto:

   ```bash
   # Windows (PowerShell ou Prompt de Comando)
   powershell -ExecutionPolicy Bypass -File scripts\seed-demo.ps1

   # Linux/macOS
   sh scripts/seed-demo.sh
   ```

   Os atalhos copiam o SQL para dentro do container `bibliotech-db` e o executam
   com `psql`. Ao final aparece um resumo como este:

   ```
              dado           | total
   --------------------------+-------
    usuarios                 |    11
    itens                    |    15
    emprestimos em andamento |     8
    emprestimos atrasados    |     3
    emprestimos concluidos   |    10
    reservas ativas          |     5
   ```

4. **Abra a aplicação** e entre com `admin@bibliotech.local` / `admin123`
   ou com o Administrador de demonstração `coordenacao` / `demo123`.

### Passo a passo (PostgreSQL nativo, sem Docker)

Com o banco criado conforme a opção B e a aplicação já aberta uma vez, execute o
SQL direto com o `psql` instalado junto do PostgreSQL:

```bash
psql -h localhost -U bibliotech -d bibliotech -v ON_ERROR_STOP=1 -f scripts/seed-demo.sql
```

No Windows, se os acentos aparecerem trocados, rode `chcp 65001` no terminal
antes do comando.

### O que é criado

As datas são calculadas a partir do dia em que o seed roda, então o cenário
sempre parece atual (empréstimos "de 3 dias atrás", "vencidos há 5 dias" etc.).
Prazo de empréstimo de 15 dias e validade de reserva de 7 dias, os valores
padrão de `database.properties`.

**Usuários** — use o CPF ou a matrícula nas telas que identificam o usuário (CU 9):

| Usuário | CPF / matrícula | Situação |
| --- | --- | --- |
| Carlos Alberto Souza | 321.654.987-01 | 3 empréstimos em aberto: **limite atingido** |
| Mariana Costa Silva | 321.654.987-02 | 1 empréstimo **atrasado**: pendência |
| Juliana Reis Mendes | 2023001 | 1 empréstimo em dia |
| Amanda Siqueira Mendes | 321.654.987-04 | 1 empréstimo em dia; 1ª da fila de O Hobbit |
| Bruno Oliveira Lopes | 2023002 | 1 empréstimo atrasado; 2º da fila de O Hobbit |
| Fernanda Rocha Dias | 321.654.987-06 | exemplar de Grande Sertão **separado para ela** |
| Rafael Gomes Pereira | 2024010 | 1 empréstimo em dia; 2º da fila de Grande Sertão |
| Patrícia Nunes Araújo | 321.654.987-08 / 2024011 | 1 em dia e 1 atrasado |
| Gabriela Torres Lima | 321.654.987-09 | só histórico encerrado: **pode ser excluída** |
| Lucas Martins Ferreira | 2025003 | sem movimentações: **pode ser excluído** |

**Acervo** — 15 itens com tombo `DEM-001` a `DEM-015` (livros, uma revista e
um DVD). O Hobbit e 1984 estão sem exemplar; Grande Sertão tem 1 exemplar, mas
reservado para Fernanda.

### Roteiro sugerido para a apresentação

| Caso de uso | O que fazer | Resultado esperado |
| --- | --- | --- |
| CU 8 | Pesquisar Acervo com o campo vazio (ou `DEM-` só para a demonstração) | itens com a disponibilidade |
| CU 10 | Empréstimo de Dom Casmurro para Juliana (2023001) | empréstimo registrado |
| CU 10, 3.1 | Selecionar O Hobbit no empréstimo | indisponível; oferece reserva |
| CU 10, 7.1 | Empréstimo para Carlos (321.654.987-01) | recusado: limite atingido |
| CU 10, 7.1 | Empréstimo para Mariana (321.654.987-02) | recusado: item em atraso |
| CU 12, 7.1 | Grande Sertão para Rafael (2024010) | recusado: separado para Fernanda |
| CU 11 | Reservar 1984 para Lucas (2025003) | entra na fila, posição 2 |
| CU 11, 3.1 | Reservar Dom Casmurro | recusado: item disponível |
| CU 12 | Devolução de O Hobbit por Mariana | concluída; exemplar separado para Amanda |
| CU 13 | Relatório de Atrasos, período "Mês atual" | atrasados e devoluções com atraso |
| CU 5 | Excluir Gabriela; depois tentar excluir Carlos | Gabriela sai; Carlos é bloqueado |

### Restaurar ou remover

- **Restaurar o cenário** depois de uma apresentação: rode o seed de novo. Ele
  apaga só os dados de demonstração (e-mails `@demo.bibliotech.local`, tombos e
  códigos `DEM-`, e as movimentações ligadas a eles) e os recria. Cadastros
  feitos por você com outros dados não são tocados.
- **Zerar o banco inteiro** (apaga **todos** os dados, inclusive os seus):
  `docker compose down -v`, depois `docker compose up -d` e abra a aplicação
  para o Flyway recriar as tabelas.

### Problemas comuns

| Mensagem | Causa e solução |
| --- | --- |
| `Schema inexistente: abra a aplicação uma vez...` | as tabelas ainda não existem; abra a aplicação uma vez e rode de novo |
| `Container bibliotech-db nao encontrado` | o banco não está no ar; rode `docker compose up -d` |
| `execução de scripts foi desabilitada` (PowerShell) | use o comando exatamente como acima, com `-ExecutionPolicy Bypass` |
| `duplicate key ... usuario_cpf_key` ou `login` | já existe um cadastro seu com o mesmo CPF ou o login `coordenacao`; altere o seu ou o valor em `seed-demo.sql` |

Em caso de erro, nada é gravado: o script roda em uma única transação.

---

## Documentação complementar

- `docs/ARQUITETURA.md` — camadas, threading no JavaFX e decisões técnicas
- `docs/MODELO-DADOS.md` — tabelas, constraints e rastreabilidade
- [docs/PENDENCIAS.md](docs/PENDENCIAS.md) — funcionalidades entregues e trabalho restante

## Interface e navegação

- **Login:** painel translúcido arredondado, campos arredondados, marca BiblioTech e
  imagem original dos livros, conforme o [Figma SGB](https://www.figma.com/design/7vOy8UHiIwO9NGVkXVkxvu/SGB?node-id=6-21).
  Aceita e-mail ou login; somente Administrador acessa o sistema.
- **Visão geral:** três indicadores reais e acesso rápido a **Gerar Relatório**,
  **Pesquisar Acervo**, **Cadastrar item**, **Realizar reserva** e **Realizar devolução**.
  Os botões se distribuem em linhas conforme a largura da janela. As consultas de empréstimos e
  reservas ficam exclusivamente em **Relatórios**.
- **Rodapé:** nome do administrador, Cadastrar Leitor, Cadastrar Empréstimo e
  sair. O menu do perfil contém somente **Administrar usuários**.
- **Relatórios:** aba **Gerar Relatório** (CU 13) com os quatro tipos do
  documento — itens emprestados, itens reservados e disponíveis, atrasos e
  histórico por usuário —, filtros próprios de cada tipo e atalhos diário,
  semanal e mensal; abas **Consultar Empréstimos** (busca, status e período de
  retirada) e **Consultar Reservas** (busca, status e ordem de posição na fila).
  Use **Pesquisar** ou Enter na busca; **Limpar filtros** volta à consulta completa.
  Carregamento, ausência de resultados e falhas têm mensagens próprias.
- **Realizar empréstimo:** segue o diagrama do CU 10 — item, verificação de
  disponibilidade, identificação do usuário e registro. Com item indisponível,
  a tela oferece a reserva; a data de retirada é a atual e não é editável.
- Todas as telas dos casos de uso estão implementadas. A exportação CSV
  continua pendente; veja `docs/PENDENCIAS.md`.

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

## Exclusão de usuário e reservas

A migration V5 permite excluir um usuário que só tem histórico encerrado: os
empréstimos e reservas antigos permanecem e aparecem como "(usuário excluído)".
Com empréstimo ou reserva ativa, a exclusão continua bloqueada.

Na devolução de um item com reserva aguardando, o exemplar fica separado para o
primeiro da fila até o fim da validade (`regra.diasValidadeReserva`); só esse
usuário consegue emprestá-lo, e a reserva passa a atendida.

Validação: 53 testes unitários e 7 testes de integração aprovados em
PostgreSQL 17 temporário, incluindo instalação limpa, atualização V3 → V4,
exclusão com histórico (V5) e rollback de transação. As 13 telas foram abertas
contra o banco local. As funcionalidades pendentes estão em `docs/PENDENCIAS.md`.
