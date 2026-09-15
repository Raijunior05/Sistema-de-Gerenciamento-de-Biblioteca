package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.model.Emprestimo;
import br.univasf.bibliotech.model.Reserva;
import br.univasf.bibliotech.model.StatusEmprestimo;
import br.univasf.bibliotech.model.StatusReserva;
import br.univasf.bibliotech.util.Datas;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

/** CU 13 - area de relatorios, com consultas de emprestimos e reservas. */
public class RelatorioController {

    @FXML private VBox areaEmprestimos;
    @FXML private VBox areaReservas;

    @FXML
    private void initialize() {
        montarEmprestimos();
        montarReservas();
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
