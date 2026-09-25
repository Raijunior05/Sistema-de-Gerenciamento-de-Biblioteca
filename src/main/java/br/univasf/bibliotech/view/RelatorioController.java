package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Emprestimo;
import br.univasf.bibliotech.model.Reserva;
import br.univasf.bibliotech.model.StatusEmprestimo;
import br.univasf.bibliotech.model.StatusReserva;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.service.RelatorioService;
import br.univasf.bibliotech.util.Datas;
import br.univasf.bibliotech.util.ExportadorCsv;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

/** CU 13 - area de relatorios, com consultas de emprestimos e reservas. */
public class RelatorioController {

    @FXML private VBox areaGerar;
    @FXML private VBox areaEmprestimos;
    @FXML private VBox areaReservas;

    // Estado do ultimo relatorio gerado — usado pela exportacao CSV
    private RelatorioService.Tipo ultimoTipo;
    private List<Emprestimo> ultimasLinhasEmprestimos;
    private List<RelatorioService.SituacaoItem> ultimasLinhasItens;

    private static final String HOJE = "Hoje (diário)";
    private static final String SEMANA = "Últimos 7 dias (semanal)";
    private static final String MES = "Mês atual (mensal)";
    private static final String TUDO = "Todo o período";
    private static final String PERSONALIZADO = "Personalizado";

    @FXML
    private void initialize() {
        montarGerarRelatorio();
        montarEmprestimos();
        montarReservas();
    }

    private static String rotulo(RelatorioService.Tipo tipo) {
        return switch (tipo) {
            case ITENS_EMPRESTADOS -> "Itens emprestados";
            case ITENS_RESERVADOS_E_DISPONIVEIS -> "Itens reservados e disponíveis";
            case ATRASOS -> "Atrasos";
            case HISTORICO_POR_USUARIO -> "Histórico de empréstimos por usuário";
        };
    }

    /** CU 13 passos 01 a 04 e fluxo alternativo 4.1. */
    private void montarGerarRelatorio() {
        // Passo 01: tipo de relatório
        ComboBox<RelatorioService.Tipo> tipo = new ComboBox<>();
        tipo.getItems().setAll(RelatorioService.Tipo.values());
        tipo.setConverter(new StringConverter<>() {
            @Override
            public String toString(RelatorioService.Tipo t) {
                return t == null ? "" : rotulo(t);
            }

            @Override
            public RelatorioService.Tipo fromString(String texto) {
                return null;
            }
        });
        tipo.setPromptText("Escolha o tipo de relatório");
        tipo.setPrefWidth(340);

        // Passo 02: filtros disponíveis para o tipo escolhido
        ComboBox<String> periodo = new ComboBox<>();
        periodo.getItems().setAll(HOJE, SEMANA, MES, TUDO, PERSONALIZADO);
        DatePicker inicio = new DatePicker();
        DatePicker fim = new DatePicker();
        inicio.setPrefWidth(145);
        fim.setPrefWidth(145);
        boolean[] aplicandoAtalho = {false};
        periodo.setOnAction(e -> {
            LocalDate hoje = LocalDate.now();
            String escolha = periodo.getValue();
            aplicandoAtalho[0] = true;
            if (HOJE.equals(escolha)) {
                inicio.setValue(hoje);
                fim.setValue(hoje);
            } else if (SEMANA.equals(escolha)) {
                inicio.setValue(hoje.minusDays(6));
                fim.setValue(hoje);
            } else if (MES.equals(escolha)) {
                inicio.setValue(hoje.withDayOfMonth(1));
                fim.setValue(hoje);
            } else if (TUDO.equals(escolha)) {
                inicio.setValue(null);
                fim.setValue(null);
            }
            aplicandoAtalho[0] = false;
        });
        periodo.setValue(MES);
        periodo.getOnAction().handle(null);
        inicio.setOnAction(e -> {
            if (!aplicandoAtalho[0]) {
                periodo.setValue(PERSONALIZADO);
            }
        });
        fim.setOnAction(inicio.getOnAction());

        TextField identificacao = campoBusca("CPF ou matrícula do usuário");
        TextField termo = campoBusca("Título, autor, categoria, ISBN ou tombo");

        VBox filtroPeriodo = rotulo("Período de retirada", periodo);
        VBox filtroInicio = rotulo("De", inicio);
        VBox filtroFim = rotulo("Até", fim);
        VBox filtroUsuario = rotulo("Usuário", identificacao);
        VBox filtroTermo = rotulo("Busca no acervo", termo);

        Button gerar = new Button("Gerar relatório");
        gerar.getStyleClass().add("botao-primario");
        gerar.setDisable(true);

        // Botão de exportação — só aparece após relatório gerado com sucesso
        Button exportarCsv = new Button("Exportar CSV");
        exportarCsv.getStyleClass().add("botao-menu");
        mostrar(exportarCsv, false);

        // Os dois botões ficam lado a lado num HBox para não quebrarem no FlowPane
        HBox botoesGerar = new HBox(8, gerar, exportarCsv);
        botoesGerar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        FlowPane filtros = new FlowPane(12, 12, rotulo("Tipo de relatório", tipo),
                filtroUsuario, filtroTermo, filtroPeriodo, filtroInicio, filtroFim, botoesGerar);

        Label cabecalho = new Label("Escolha o tipo de relatório para exibir os filtros.");
        cabecalho.getStyleClass().add("rotulo-campo");
        cabecalho.setWrapText(true);
        cabecalho.setMinHeight(Region.USE_PREF_SIZE);
        Label resumo = new Label();
        resumo.getStyleClass().add("rotulo-secao");
        resumo.setWrapText(true);
        resumo.setMinHeight(Region.USE_PREF_SIZE);

        // Colunas mudam a cada tipo; a politica restrita espreme colunas recriadas.
        TableView<Emprestimo> tabelaEmprestimos = tabela();
        tabelaEmprestimos.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        TableView<RelatorioService.SituacaoItem> tabelaItens = tabela();
        tabelaItens.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        Runnable exibirFiltros = () -> {
            RelatorioService.Tipo t = tipo.getValue();
            boolean itens = t == RelatorioService.Tipo.ITENS_RESERVADOS_E_DISPONIVEIS;
            mostrar(filtroUsuario, t == RelatorioService.Tipo.HISTORICO_POR_USUARIO);
            mostrar(filtroTermo, itens);
            mostrar(filtroPeriodo, t != null && !itens);
            mostrar(filtroInicio, t != null && !itens);
            mostrar(filtroFim, t != null && !itens);
            gerar.setDisable(t == null);
            cabecalho.setText(t == null
                    ? "Escolha o tipo de relatório para exibir os filtros."
                    : "Preencha os filtros e clique em \"Gerar relatório\".");
            resumo.setText("");
            // Ao trocar o tipo, o relatório anterior deixa de ser válido
            mostrar(exportarCsv, false);
            ultimoTipo = null;
            ultimasLinhasEmprestimos = null;
            ultimasLinhasItens = null;
            mostrar(tabelaEmprestimos, t != null && !itens);
            mostrar(tabelaItens, itens);
            tabelaEmprestimos.getColumns().clear();
            tabelaEmprestimos.getItems().clear();
            tabelaItens.getColumns().clear();
            tabelaItens.getItems().clear();
        };
        tipo.setOnAction(e -> exibirFiltros.run());
        exibirFiltros.run();

        // Passos 03 e 04: filtros lidos na thread JavaFX; consulta em segundo plano
        Runnable gerarRelatorio = () -> {
            RelatorioService.Tipo t = tipo.getValue();
            LocalDate de = inicio.getValue();
            LocalDate ate = fim.getValue();
            String documento = identificacao.getText().trim();
            String busca = termo.getText().trim();
            if (t == RelatorioService.Tipo.HISTORICO_POR_USUARIO && documento.isBlank()) {
                resumo.setText("Informe o CPF ou a matrícula do usuário.");
                return;
            }

            mostrar(exportarCsv, false);
            filtros.setDisable(true);
            resumo.setText("Gerando relatório…");
            Task<Object> tarefa = new Task<>() {
                @Override
                protected Object call() {
                    var relatorios = App.servicos().relatorios();
                    return switch (t) {
                        case ITENS_EMPRESTADOS -> relatorios.itensEmprestados(de, ate);
                        case ATRASOS -> relatorios.atrasos(de, ate);
                        case HISTORICO_POR_USUARIO -> new Historico(
                                App.servicos().usuarios().identificar(documento),
                                de, ate);
                        case ITENS_RESERVADOS_E_DISPONIVEIS ->
                                relatorios.itensReservadosEDisponiveis(busca);
                    };
                }
            };
            tarefa.setOnSucceeded(e -> {
                filtros.setDisable(false);
                exibirRelatorio(t, tarefa.getValue(), de, ate, cabecalho, resumo,
                        tabelaEmprestimos, tabelaItens);
                // Guarda estado para exportação e exibe o botão
                mostrar(exportarCsv, true);
            });
            tarefa.setOnFailed(e -> {
                filtros.setDisable(false);
                mostrar(exportarCsv, false);
                Throwable causa = tarefa.getException();
                if (causa instanceof RegraNegocioException) {
                    resumo.setText(causa.getMessage());
                } else {
                    resumo.setText("Falha ao gerar o relatório. Tente novamente.");
                    Alertas.erro("Não foi possível gerar o relatório.", causa);
                }
            });
            Thread thread = new Thread(tarefa, "gerar-relatorio");
            thread.setDaemon(true);
            thread.start();
        };
        gerar.setOnAction(e -> gerarRelatorio.run());
        identificacao.setOnAction(e -> gerarRelatorio.run());
        termo.setOnAction(e -> gerarRelatorio.run());

        // Ação do botão Exportar CSV
        exportarCsv.setOnAction(e -> exportarCsvAtual(exportarCsv));

        areaGerar.getChildren().addAll(filtros, cabecalho, resumo, tabelaEmprestimos, tabelaItens);
    }

    /**
     * Abre FileChooser e grava o ultimo relatorio gerado em CSV em thread de fundo.
     * Trata cancelamento (usuario fecha o dialogo) e falha de I/O.
     *
     * @param botao botao que disparou a acao (usado para obter o Stage)
     */
    @SuppressWarnings("unchecked")
    private void exportarCsvAtual(Button botao) {
        if (ultimoTipo == null) {
            return;
        }

        String nomeArquivo = "relatorio_"
                + ultimoTipo.name().toLowerCase()
                + "_" + Datas.formatar(LocalDate.now()).replace("/", "-")
                + ".csv";

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar relatório CSV");
        chooser.setInitialFileName(nomeArquivo);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivo CSV (*.csv)", "*.csv"));

        Stage stage = (Stage) botao.getScene().getWindow();
        File destino = chooser.showSaveDialog(stage);

        // Usuario cancelou o dialogo — nao faz nada
        if (destino == null) {
            return;
        }

        // Monta cabeçalho e linhas de acordo com o tipo do ultimo relatorio
        final String[] cabecalho;
        final List<String[]> linhas = new ArrayList<>();
        boolean itens = ultimoTipo == RelatorioService.Tipo.ITENS_RESERVADOS_E_DISPONIVEIS;

        if (itens) {
            cabecalho = new String[]{"Tombo", "Título", "Autor",
                    "Disponíveis / Total", "Aguardando na fila", "Separados p/ retirada"};
            for (RelatorioService.SituacaoItem s : ultimasLinhasItens) {
                linhas.add(new String[]{
                        s.item().getTombo(),
                        s.item().getTitulo(),
                        s.item().getAutor(),
                        s.item().getQuantidadeDisponivel() + " / " + s.item().getQuantidadeTotal(),
                        String.valueOf(s.reservasAguardando()),
                        String.valueOf(s.reservasParaRetirada())
                });
            }
        } else {
            boolean comUsuario = ultimoTipo != RelatorioService.Tipo.HISTORICO_POR_USUARIO;
            boolean comDevolucao = ultimoTipo != RelatorioService.Tipo.ITENS_EMPRESTADOS;
            LocalDate hoje = LocalDate.now();

            List<String> cols = new ArrayList<>();
            if (comUsuario) {
                cols.add("Usuário");
            }
            cols.add("Código");
            cols.add("Item");
            cols.add("Retirada");
            cols.add("Prazo");
            if (comDevolucao) {
                cols.add("Devolução");
                cols.add("Dias de atraso");
            }
            cols.add("Status");
            cabecalho = cols.toArray(new String[0]);

            for (Emprestimo emp : ultimasLinhasEmprestimos) {
                List<String> vals = new ArrayList<>();
                if (comUsuario) {
                    vals.add(emp.getUsuario().getNome());
                }
                vals.add(emp.getCodigo());
                vals.add(emp.getItem().getTitulo());
                vals.add(Datas.formatar(emp.getDataEmprestimo()));
                vals.add(Datas.formatar(emp.getDataPrevista()));
                if (comDevolucao) {
                    vals.add(Datas.formatar(emp.getDataDevolucao()));
                    vals.add(String.valueOf(emp.calcularDiasAtraso(hoje)));
                }
                vals.add(emp.getStatus().getRotulo());
                linhas.add(vals.toArray(new String[0]));
            }
        }

        // Grava em thread de fundo para não bloquear a UI
        Path caminhoDestino = destino.toPath();
        botao.setDisable(true);
        Task<Void> tarefa = new Task<>() {
            @Override
            protected Void call() throws IOException {
                ExportadorCsv.gravar(caminhoDestino, cabecalho, linhas);
                return null;
            }
        };
        tarefa.setOnSucceeded(ev -> {
            botao.setDisable(false);
            Alertas.sucesso("Exportação concluída",
                    "Arquivo salvo em:\n" + caminhoDestino.toAbsolutePath());
        });
        tarefa.setOnFailed(ev -> {
            botao.setDisable(false);
            Alertas.erro("Não foi possível salvar o arquivo CSV.", tarefa.getException());
        });
        Thread thread = new Thread(tarefa, "exportar-csv");
        thread.setDaemon(true);
        thread.start();
    }

    /** Resultado do histórico: o usuário identificado e a lista gerada em segundo plano. */
    private record Historico(Usuario usuario, List<Emprestimo> emprestimos) {
        Historico(Usuario usuario, LocalDate de, LocalDate ate) {
            this(usuario, App.servicos().relatorios().historicoPorUsuario(usuario, de, ate));
        }
    }

    @SuppressWarnings("unchecked")
    private void exibirRelatorio(RelatorioService.Tipo tipo, Object resultado,
                                 LocalDate de, LocalDate ate, Label cabecalho, Label resumo,
                                 TableView<Emprestimo> tabelaEmprestimos,
                                 TableView<RelatorioService.SituacaoItem> tabelaItens) {
        LocalDate hoje = LocalDate.now();
        String periodo = de == null && ate == null ? "todo o período"
                : (de == null ? "início" : Datas.formatar(de)) + " a "
                        + (ate == null ? "hoje" : Datas.formatar(ate));
        String titulo = "Relatório: " + rotulo(tipo);
        boolean itens = tipo == RelatorioService.Tipo.ITENS_RESERVADOS_E_DISPONIVEIS;
        mostrar(tabelaItens, itens);
        mostrar(tabelaEmprestimos, !itens);
        tabelaEmprestimos.getColumns().clear();
        tabelaItens.getColumns().clear();

        int registros;
        String totais;
        if (itens) {
            var linhas = (List<RelatorioService.SituacaoItem>) resultado;
            coluna(tabelaItens, "Tombo", s -> s.item().getTombo());
            coluna(tabelaItens, "Título", s -> s.item().getTitulo());
            coluna(tabelaItens, "Autor", s -> s.item().getAutor());
            coluna(tabelaItens, "Disponíveis", s -> s.item().getQuantidadeDisponivel()
                    + " / " + s.item().getQuantidadeTotal());
            coluna(tabelaItens, "Aguardando na fila", s -> String.valueOf(s.reservasAguardando()));
            coluna(tabelaItens, "Separados p/ retirada", s -> String.valueOf(s.reservasParaRetirada()));
            tabelaItens.getItems().setAll(linhas);
            registros = linhas.size();
            totais = registros + " item(ns) · "
                    + linhas.stream().mapToInt(s -> s.item().getQuantidadeDisponivel()).sum()
                    + " exemplar(es) disponível(is) · "
                    + linhas.stream().mapToInt(s -> s.reservasAguardando() + s.reservasParaRetirada()).sum()
                    + " reserva(s) ativa(s)";
        } else {
            List<Emprestimo> lista;
            if (tipo == RelatorioService.Tipo.HISTORICO_POR_USUARIO) {
                Historico h = (Historico) resultado;
                lista = h.emprestimos();
                titulo += " — " + h.usuario().getNome();
            } else {
                lista = (List<Emprestimo>) resultado;
                coluna(tabelaEmprestimos, "Usuário", e -> e.getUsuario().getNome());
            }
            coluna(tabelaEmprestimos, "Código", Emprestimo::getCodigo);
            coluna(tabelaEmprestimos, "Item", e -> e.getItem().getTitulo());
            coluna(tabelaEmprestimos, "Retirada", e -> Datas.formatar(e.getDataEmprestimo()));
            coluna(tabelaEmprestimos, "Prazo", e -> Datas.formatar(e.getDataPrevista()));
            if (tipo != RelatorioService.Tipo.ITENS_EMPRESTADOS) {
                coluna(tabelaEmprestimos, "Devolução", e -> Datas.formatar(e.getDataDevolucao()));
                coluna(tabelaEmprestimos, "Dias de atraso",
                        e -> String.valueOf(e.calcularDiasAtraso(hoje)));
            }
            coluna(tabelaEmprestimos, "Status", e -> e.getStatus().getRotulo());
            tabelaEmprestimos.getItems().setAll(lista);
            registros = lista.size();
            long emAtraso = lista.stream().filter(e -> e.calcularDiasAtraso(hoje) > 0).count();
            totais = registros + " empréstimo(s) · " + emAtraso + " com atraso";
        }

        cabecalho.setText(titulo + "\nPeríodo: " + (itens ? "situação atual" : periodo)
                + " · Gerado em " + Datas.formatar(hoje));
        // Fluxo 4.1: dados insuficientes para exibição
        resumo.setText(registros == 0
                ? "Não existem registros para os filtros informados."
                : "Resumo: " + totais + ".");

        // Salva estado para exportacao CSV posterior
        ultimoTipo = tipo;
        if (itens) {
            ultimasLinhasItens = (List<RelatorioService.SituacaoItem>) resultado;
            ultimasLinhasEmprestimos = null;
        } else {
            ultimasLinhasEmprestimos = tipo == RelatorioService.Tipo.HISTORICO_POR_USUARIO
                    ? ((Historico) resultado).emprestimos()
                    : (List<Emprestimo>) resultado;
            ultimasLinhasItens = null;
        }
    }

    private static void mostrar(javafx.scene.Node no, boolean visivel) {
        no.setVisible(visivel);
        no.setManaged(visivel);
    }

    private void montarEmprestimos() {
        TextField busca = campoBusca("Leitor, obra ou código");
        ComboBox<StatusEmprestimo> status = new ComboBox<>();
        status.getItems().add(null);
        status.getItems().addAll(StatusEmprestimo.values());
        status.setPromptText("Todos os status");
        DatePicker inicio = new DatePicker();
        DatePicker fim = new DatePicker();
        inicio.setEditable(false);
        fim.setEditable(false);
        inicio.setPrefWidth(145);
        fim.setPrefWidth(145);
        Button pesquisar = botaoPesquisar();
        Button limpar = new Button("Limpar filtros");
        limpar.getStyleClass().add("botao-menu");
        Label mensagem = new Label();
        mensagem.getStyleClass().add("rotulo-secao");
        TableView<Emprestimo> tabela = tabela();
        coluna(tabela, "Código", Emprestimo::getCodigo);
        coluna(tabela, "Leitor", e -> e.getUsuario().getNome());
        coluna(tabela, "Livro / Obra", e -> e.getItem().getTitulo());
        coluna(tabela, "Retirada", e -> Datas.formatar(e.getDataEmprestimo()));
        coluna(tabela, "Prazo", e -> Datas.formatar(e.getDataPrevista()));
        coluna(tabela, "Devolução", e -> Datas.formatar(e.getDataDevolucao()));
        coluna(tabela, "Status", e -> e.getStatus().getRotulo());
        FlowPane filtros = new FlowPane(12, 12, rotulo("Busca rápida", busca), rotulo("Status", status),
                rotulo("Retirada de", inicio), rotulo("Até", fim), pesquisar, limpar);
        Runnable consultar = () -> {
            LocalDate de = inicio.getValue();
            LocalDate ate = fim.getValue();
            if (de != null && ate != null && de.isAfter(ate)) {
                mensagem.setText("A data inicial deve ser anterior ou igual à final.");
                return;
            }
            String termo = busca.getText().trim();
            StatusEmprestimo filtro = status.getValue();
            carregar(tabela, filtros, mensagem,
                    () -> App.servicos().emprestimos().pesquisar(termo, filtro, de, ate));
        };
        pesquisar.setOnAction(e -> consultar.run());
        busca.setOnAction(e -> consultar.run());
        limpar.setOnAction(e -> {
            busca.clear();
            status.setValue(null);
            inicio.setValue(null);
            fim.setValue(null);
            consultar.run();
        });
        areaEmprestimos.getChildren().addAll(filtros, mensagem, tabela);
        consultar.run();
    }

    private void montarReservas() {
        TextField busca = campoBusca("Leitor ou obra");
        ComboBox<StatusReserva> status = new ComboBox<>();
        status.getItems().add(null);
        status.getItems().addAll(StatusReserva.values());
        status.setPromptText("Todos os status");
        CheckBox crescente = new CheckBox("Posição crescente na fila");
        crescente.setSelected(true);
        Button pesquisar = botaoPesquisar();
        Button limpar = new Button("Limpar filtros");
        limpar.getStyleClass().add("botao-menu");
        Label mensagem = new Label();
        mensagem.getStyleClass().add("rotulo-secao");
        TableView<Reserva> tabela = tabela();
        coluna(tabela, "Posição", Reserva::getRotuloPosicao);
        coluna(tabela, "Leitor", r -> r.getUsuario().getNome());
        coluna(tabela, "Livro / Obra", r -> r.getItem().getTitulo());
        coluna(tabela, "Data da reserva", r -> Datas.formatar(r.getDataReserva()));
        coluna(tabela, "Validade", r -> Datas.formatar(r.getValidadeMaxima()));
        coluna(tabela, "Status", r -> r.getStatus().getRotulo());
        FlowPane filtros = new FlowPane(12, 12, rotulo("Busca rápida", busca), rotulo("Status", status),
                crescente, pesquisar, limpar);
        Runnable consultar = () -> {
            String termo = busca.getText().trim();
            StatusReserva filtro = status.getValue();
            boolean ordenar = crescente.isSelected();
            carregar(tabela, filtros, mensagem,
                    () -> App.servicos().reservas().pesquisar(termo, filtro, ordenar));
        };
        pesquisar.setOnAction(e -> consultar.run());
        busca.setOnAction(e -> consultar.run());
        limpar.setOnAction(e -> {
            busca.clear();
            status.setValue(null);
            crescente.setSelected(true);
            consultar.run();
        });
        areaReservas.getChildren().addAll(filtros, mensagem, tabela);
        consultar.run();
    }

    private TextField campoBusca(String dica) {
        TextField campo = new TextField();
        campo.setPromptText(dica);
        campo.setPrefWidth(220);
        return campo;
    }

    private VBox rotulo(String texto, javafx.scene.Node campo) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("rotulo-campo");
        rotulo.setLabelFor(campo);
        return new VBox(6, rotulo, campo);
    }

    private Button botaoPesquisar() {
        Button botao = new Button("Pesquisar");
        botao.getStyleClass().add("botao-primario");
        return botao;
    }

    private <T> TableView<T> tabela() {
        TableView<T> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nenhum registro encontrado."));
        VBox.setVgrow(tabela, Priority.ALWAYS);
        return tabela;
    }

    private <T> void coluna(TableView<T> tabela, String titulo, Function<T, String> valor) {
        TableColumn<T, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(c -> new ReadOnlyStringWrapper(valor.apply(c.getValue())));
        coluna.setPrefWidth(140);
        tabela.getColumns().add(coluna);
    }

    private <T> void carregar(TableView<T> tabela, FlowPane filtros, Label mensagem,
                             Callable<List<T>> consulta) {
        filtros.setDisable(true);
        tabela.getItems().clear();
        tabela.setPlaceholder(new Label("Carregando…"));
        mensagem.setText("Consultando registros…");
        Task<List<T>> tarefa = new Task<>() {
            @Override
            protected List<T> call() throws Exception {
                return consulta.call();
            }
        };
        tarefa.setOnSucceeded(e -> {
            tabela.getItems().setAll(tarefa.getValue());
            tabela.setPlaceholder(new Label("Nenhum registro encontrado."));
            mensagem.setText(tarefa.getValue().size() + " registro(s) encontrado(s).");
            filtros.setDisable(false);
        });
        tarefa.setOnFailed(e -> {
            filtros.setDisable(false);
            tabela.setPlaceholder(new Label("Não foi possível carregar os registros."));
            mensagem.setText("Falha na consulta. Tente novamente.");
            Alertas.erro("Não foi possível consultar os registros.", tarefa.getException());
        });
        Thread thread = new Thread(tarefa, "consulta-relatorio");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onVoltar() {
        Navegador.irPara(Navegador.Tela.VISAO_GERAL);
    }
}
