package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Emprestimo;
import br.univasf.bibliotech.model.Reserva;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.util.Datas;
import br.univasf.bibliotech.util.Sessao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class DevolucaoController {

    @FXML private TextField campoIdentificacao;
    @FXML private Button botaoIdentificar;
    @FXML private Button botaoConfirmar;

    @FXML private Label rotuloUsuario;
    @FXML private Label rodapeNome;
    @FXML private Label rotuloPrazo;
    @FXML private Label rotuloAtraso;
    @FXML private Label mensagemErro;
    @FXML private Label mensagemErroTabela;

    @FXML private VBox areaIdentificacao;
    @FXML private VBox areaEmprestimos;
    @FXML private VBox areaDetalhes;

    @FXML private TableView<Emprestimo> tabelaEmprestimos;
    @FXML private TableColumn<Emprestimo, String> colunaCodigo;
    @FXML private TableColumn<Emprestimo, String> colunaLeitor;
    @FXML private TableColumn<Emprestimo, String> colunaTitulo;
    @FXML private TableColumn<Emprestimo, String> colunaRetirada;
    @FXML private TableColumn<Emprestimo, String> colunaPrazo;
    @FXML private TableColumn<Emprestimo, String> colunaStatus;
    @FXML private TableColumn<Emprestimo, String> colunaAcoes;

    private Usuario usuarioIdentificado;

    @FXML
    private void initialize() {
        rodapeNome.setText(
                Sessao.getAdministrador().getNome()
        );

        colunaCodigo.setCellValueFactory(dados ->
                texto(dados.getValue().getCodigo())
        );

        colunaLeitor.setCellValueFactory(dados ->
                texto(dados.getValue().getUsuario().getNome())
        );

        colunaTitulo.setCellValueFactory(dados ->
                texto(dados.getValue().getItem().getTitulo())
        );

        colunaRetirada.setCellValueFactory(dados ->
                texto(Datas.formatar(
                        dados.getValue().getDataEmprestimo()
                ))
        );

        colunaPrazo.setCellValueFactory(dados ->
                texto(Datas.formatar(
                        dados.getValue().getDataPrevista()
                ))
        );

        colunaStatus.setCellValueFactory(dados ->
                texto(
                        diasAtraso(dados.getValue()) > 0
                                ? "Atrasado"
                                : "Ativo"
                )
        );

        colunaAcoes.setCellValueFactory(dados ->
                texto("acao")
        );

        colunaAcoes.setCellFactory(coluna -> new TableCell<>() {

            private final Button botao = new Button("◉");

            {
                botao.getStyleClass().add(
                        "botao-acao-devolucao"
                );

                botao.setFocusTraversable(false);

                botao.setOnAction(evento -> {
                    Emprestimo emprestimo =
                            getTableRow().getItem();

                    if (emprestimo != null) {
                        tabelaEmprestimos
                                .getSelectionModel()
                                .select(emprestimo);

                        mostrarSelecao(emprestimo);
                    }
                });
            }

            @Override
            protected void updateItem(
                    String item,
                    boolean vazio
            ) {
                super.updateItem(item, vazio);

                setGraphic(vazio ? null : botao);
                setText(null);
            }
        });

        tabelaEmprestimos.setPlaceholder(
                new Label(
                        "Identifique o usuário para consultar empréstimos."
                )
        );

        tabelaEmprestimos
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, atual) ->
                        mostrarSelecao(atual)
                );

        campoIdentificacao
                .textProperty()
                .addListener((obs, anterior, atual) ->
                        limparConsulta()
                );

        mostrarEtapa(false);
        mostrarSelecao(null);
    }

    private ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(
                valor == null ? "" : valor
        );
    }

    private long diasAtraso(Emprestimo emprestimo) {
        return emprestimo.calcularDiasAtraso(
                LocalDate.now()
        );
    }

    private void mostrarEtapa(boolean identificado) {
        areaIdentificacao.setVisible(!identificado);
        areaIdentificacao.setManaged(!identificado);

        areaEmprestimos.setVisible(identificado);
        areaEmprestimos.setManaged(identificado);
    }

    private void limparConsulta() {
        usuarioIdentificado = null;

        rotuloUsuario.setText(
                "Nenhum usuário identificado"
        );

        tabelaEmprestimos.getItems().clear();

        tabelaEmprestimos
                .getSelectionModel()
                .clearSelection();

        tabelaEmprestimos.setPlaceholder(
                new Label(
                        "Identifique o usuário para consultar empréstimos."
                )
        );

        mostrarSelecao(null);
    }

    @FXML
    private void onIdentificar() {
        String documento =
                campoIdentificacao.getText() == null
                        ? ""
                        : campoIdentificacao.getText().trim();

        mensagemErro.setText("");
        mensagemErroTabela.setText("");
        limparConsulta();

        if (documento.isBlank()) {
            mensagemErro.setText(
                    "Informe o CPF ou a matrícula do usuário."
            );
            return;
        }

        botaoIdentificar.setDisable(true);
        campoIdentificacao.setDisable(true);

        tabelaEmprestimos.setPlaceholder(
                new Label("Carregando empréstimos...")
        );

        Task<Consulta> tarefa = new Task<>() {
            @Override
            protected Consulta call() {
                Usuario usuario = App.servicos()
                        .usuarios()
                        .identificar(documento);

                List<Emprestimo> abertos = App.servicos()
                        .emprestimos()
                        .listarAbertos(usuario.getId());

                return new Consulta(usuario, abertos);
            }
        };

        tarefa.setOnSucceeded(evento -> {
            botaoIdentificar.setDisable(false);
            campoIdentificacao.setDisable(false);

            usuarioIdentificado =
                    tarefa.getValue().usuario();

            rotuloUsuario.setText(
                    "Usuário: "
                            + usuarioIdentificado.getNome()
            );

            tabelaEmprestimos.getItems().setAll(
                    tarefa.getValue().emprestimos()
            );

            tabelaEmprestimos.setPlaceholder(
                    new Label(
                            "Nenhum empréstimo aberto para este usuário."
                    )
            );

            mostrarEtapa(true);
        });

        tarefa.setOnFailed(evento -> {
            botaoIdentificar.setDisable(false);
            campoIdentificacao.setDisable(false);

            tabelaEmprestimos.setPlaceholder(
                    new Label(
                            "Não foi possível carregar os empréstimos."
                    )
            );

            Throwable causa = tarefa.getException();

            if (causa instanceof RegraNegocioException) {
                mensagemErro.setText(causa.getMessage());
            } else {
                Alertas.erro(
                        "Não foi possível identificar o usuário.",
                        causa
                );
            }
        });

        iniciar(
                tarefa,
                "identificar-usuario-devolucao"
        );
    }

    private void mostrarSelecao(Emprestimo emprestimo) {
        areaDetalhes.setVisible(
                emprestimo != null
        );

        areaDetalhes.setManaged(
                emprestimo != null
        );

        botaoConfirmar.setDisable(
                emprestimo == null
        );

        rotuloPrazo.setText(
                emprestimo == null
                        ? "Prazo: —"
                        : "Prazo: "
                                + Datas.formatar(
                                        emprestimo.getDataPrevista()
                                )
        );

        long dias = emprestimo == null
                ? 0
                : diasAtraso(emprestimo);

        rotuloAtraso.setText(
                emprestimo == null
                        ? "Atraso: —"
                        : dias == 0
                                ? "Sem atraso"
                                : "Atraso: "
                                        + dias
                                        + " dia(s)"
        );
    }

    @FXML
    private void onConfirmar() {
        Emprestimo selecionado = tabelaEmprestimos
                .getSelectionModel()
                .getSelectedItem();

        if (usuarioIdentificado == null
                || selecionado == null) {
            mensagemErroTabela.setText(
                    "Selecione um empréstimo aberto."
            );
            return;
        }

        long dias = diasAtraso(selecionado);

        String detalhes =
                "Usuário: "
                        + usuarioIdentificado.getNome()
                        + "\nObra: "
                        + selecionado.getItem().getTitulo()
                        + "\nPrazo: "
                        + Datas.formatar(
                                selecionado.getDataPrevista()
                        )
                        + "\nDias de atraso: "
                        + dias
                        + "\n\nConfirmar devolução?";

        if (!Alertas.confirmar(
                "Registrar devolução",
                detalhes
        )) {
            return;
        }

        botaoConfirmar.setDisable(true);
        botaoIdentificar.setDisable(true);
        campoIdentificacao.setDisable(true);
        tabelaEmprestimos.setDisable(true);

        mensagemErroTabela.setText("");

        Task<Optional<Reserva>> tarefa = new Task<>() {
            @Override
            protected Optional<Reserva> call() {
                return App.servicos()
                        .emprestimos()
                        .registrarDevolucao(selecionado);
            }
        };

        tarefa.setOnSucceeded(evento -> {
            botaoIdentificar.setDisable(false);
            campoIdentificacao.setDisable(false);
            tabelaEmprestimos.setDisable(false);

            tabelaEmprestimos
                    .getItems()
                    .remove(selecionado);

            String aviso = tarefa.getValue()
                    .map(reserva ->
                            "\nHá uma reserva na fila para esta obra. "
                                    + "Prioridade: "
                                    + reserva.getUsuario().getNome()
                                    + "."
                    )
                    .orElse("");

            Alertas.sucesso(
                    "Devolução registrada",
                    "Empréstimo concluído. Dias de atraso: "
                            + selecionado.getDiasAtraso()
                            + "."
                            + aviso
            );
        });

        tarefa.setOnFailed(evento -> {
            botaoIdentificar.setDisable(false);
            campoIdentificacao.setDisable(false);
            tabelaEmprestimos.setDisable(false);

            mostrarSelecao(
                    tabelaEmprestimos
                            .getSelectionModel()
                            .getSelectedItem()
            );

            Throwable causa = tarefa.getException();

            if (causa instanceof RegraNegocioException) {
                mensagemErroTabela.setText(
                        causa.getMessage()
                );
            } else {
                Alertas.erro(
                        "Não foi possível registrar a devolução.",
                        causa
                );
            }
        });

        iniciar(tarefa, "registrar-devolucao");
    }

    private void iniciar(Task<?> tarefa, String nome) {
        Thread thread = new Thread(tarefa, nome);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onVoltar() {
        if (areaEmprestimos.isVisible()) {
            mostrarEtapa(false);
            limparConsulta();
        } else {
            Navegador.irPara(
                    Navegador.Tela.VISAO_GERAL
            );
        }
    }

    private record Consulta(
            Usuario usuario,
            List<Emprestimo> emprestimos
    ) {
    }
}