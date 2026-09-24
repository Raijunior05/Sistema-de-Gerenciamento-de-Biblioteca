package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Item;
import br.univasf.bibliotech.model.Reserva;
import br.univasf.bibliotech.model.StatusReserva;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.util.Datas;
import br.univasf.bibliotech.util.Sessao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

public class ReservaController {

    // Formulário de reserva
    @FXML private VBox areaFormulario;
    @FXML private ComboBox<Item> comboItem;
    @FXML private Button botaoVerificar;
    @FXML private Button botaoEmprestimo;
    @FXML private Label rotuloDisponibilidade;
    @FXML private TextField campoIdentificacao;
    @FXML private Button botaoIdentificar;
    @FXML private Label rotuloUsuario;
    @FXML private Button botaoConfirmar;
    @FXML private Label mensagemErro;

    // Consulta após registrar a reserva
    @FXML private VBox areaConsulta;
    @FXML private TextField campoBusca;
    @FXML private ComboBox<StatusReserva> comboStatus;
    @FXML private ComboBox<String> comboOrdem;
    @FXML private Button botaoPesquisar;
    @FXML private TableView<Reserva> tabelaReservas;
    @FXML private TableColumn<Reserva, String> colunaPosicao;
    @FXML private TableColumn<Reserva, String> colunaLeitor;
    @FXML private TableColumn<Reserva, String> colunaObra;
    @FXML private TableColumn<Reserva, String> colunaData;
    @FXML private TableColumn<Reserva, String> colunaValidade;
    @FXML private TableColumn<Reserva, String> colunaStatus;
    @FXML private TableColumn<Reserva, String> colunaAcoes;
    @FXML private Label mensagemConsulta;
    @FXML private Label rodapeNome;

    private Item itemIndisponivel;
    private Usuario usuarioIdentificado;
    private Item itemRecebido;
    private Usuario usuarioRecebido;
    private String documentoRecebido;
    private boolean processando;

    public void preencherDoEmprestimo(Item item, Usuario usuario, String documento) {
        itemRecebido = item;
        usuarioRecebido = usuario;
        documentoRecebido = documento;
        if (documento != null) campoIdentificacao.setText(documento);
    }

    @FXML
    private void initialize() {
        rodapeNome.setText(
                Sessao.getAdministrador().getNome()
        );

        configurarConsulta();

        comboItem.valueProperty().addListener(
                (obs, anterior, atual) -> {
                    itemIndisponivel = null;
                    usuarioIdentificado = null;

                    rotuloUsuario.setText(
                            "Nenhum usuário identificado"
                    );

                    rotuloDisponibilidade.setText(
                            "Selecione o item e verifique a disponibilidade."
                    );

                    botaoEmprestimo.setVisible(false);
                    botaoEmprestimo.setManaged(false);

                    atualizarBotoes();
                }
        );

        campoIdentificacao.textProperty().addListener(
                (obs, anterior, atual) -> {
                    usuarioIdentificado = null;
                    usuarioRecebido = null;

                    rotuloUsuario.setText(
                            "Nenhum usuário identificado"
                    );

                    atualizarBotoes();
                }
        );

        botaoEmprestimo.setVisible(false);
        botaoEmprestimo.setManaged(false);

        carregarItens();
        atualizarBotoes();
    }

    private void carregarItens() {
        comboItem.setDisable(true);
        comboItem.setPromptText("Carregando acervo...");

        Task<List<Item>> tarefa = new Task<>() {
            @Override
            protected List<Item> call() {
                return App.servicos()
                        .itens()
                        .listarTodos();
            }
        };

        tarefa.setOnSucceeded(evento -> {
            comboItem.getItems().setAll(
                    tarefa.getValue()
            );

            comboItem.setDisable(false);

            comboItem.setPromptText(
                    tarefa.getValue().isEmpty()
                            ? "Nenhum item cadastrado"
                            : "Selecione uma obra"
            );
            if (itemRecebido != null) {
                if (comboItem.getItems().stream().noneMatch(item -> item.getId().equals(itemRecebido.getId()))) {
                    mensagemErro.setText("O item do empréstimo não está mais no acervo.");
                }
                comboItem.getItems().stream()
                        .filter(item -> item.getId().equals(itemRecebido.getId()))
                        .findFirst().ifPresent(item -> {
                            comboItem.setValue(item);
                            onVerificarItem();
                        });
                itemRecebido = null;
            }
        });

        tarefa.setOnFailed(evento -> {
            comboItem.setPromptText(
                    "Não foi possível carregar o acervo"
            );

            Alertas.erro(
                    "Não foi possível carregar os itens.",
                    tarefa.getException()
            );
        });

        iniciar(tarefa, "carregar-itens-reserva");
    }

    // CU 11: selecionar o item e verificar disponibilidade.
    @FXML
    private void onVerificarItem() {
        Item selecionado = comboItem.getValue();

        mensagemErro.setText("");
        itemIndisponivel = null;
        usuarioIdentificado = null;

        rotuloUsuario.setText(
                "Nenhum usuário identificado"
        );

        botaoEmprestimo.setVisible(false);
        botaoEmprestimo.setManaged(false);

        atualizarBotoes();

        if (selecionado == null) {
            mensagemErro.setText(
                    "Selecione um item do acervo."
            );
            return;
        }

        comboItem.setDisable(true);
        botaoVerificar.setDisable(true);

        // CU 11 passo 03: exemplar separado para o 1o da fila não conta como livre
        Task<Integer> tarefa = new Task<>() {
            @Override
            protected Integer call() {
                return App.servicos()
                        .emprestimos()
                        .verificarDisponibilidade(selecionado.getId());
            }
        };

        tarefa.setOnSucceeded(evento -> {
            comboItem.setDisable(false);
            botaoVerificar.setDisable(false);

            Item itemAtual = selecionado;

            if (tarefa.getValue() > 0) {
                // Fluxo alternativo 3.1
                rotuloDisponibilidade.setText(
                        "Item disponível: a reserva não é permitida. "
                                + "Realize um empréstimo."
                );

                botaoEmprestimo.setVisible(true);
                botaoEmprestimo.setManaged(true);
            } else {
                itemIndisponivel = itemAtual;

                rotuloDisponibilidade.setText(
                        "Item indisponível. Identifique o usuário "
                                + "para reservar."
                );

                botaoEmprestimo.setVisible(false);
                botaoEmprestimo.setManaged(false);
                if (usuarioRecebido != null) {
                    usuarioIdentificado = usuarioRecebido;
                    rotuloUsuario.setText(DadosUsuario.formatar(usuarioRecebido));
                    usuarioRecebido = null;
                } else if (documentoRecebido != null && !documentoRecebido.isBlank()) {
                    documentoRecebido = null;
                    onIdentificar();
                    return;
                }
            }

            atualizarBotoes();
        });

        tarefa.setOnFailed(evento -> {
            comboItem.setDisable(false);
            botaoVerificar.setDisable(false);

            Alertas.erro(
                    "Não foi possível verificar o item.",
                    tarefa.getException()
            );
        });

        iniciar(tarefa, "verificar-item-reserva");
    }

    // CU 11, passo 04: identificar usuário pelo CU 9.
    @FXML
    private void onIdentificar() {
        if (itemIndisponivel == null) {
            mensagemErro.setText(
                    "Verifique primeiro um item indisponível."
            );
            return;
        }

        String documento =
                campoIdentificacao.getText() == null
                        ? ""
                        : campoIdentificacao.getText().trim();

        mensagemErro.setText("");
        usuarioIdentificado = null;

        rotuloUsuario.setText(
                "Nenhum usuário identificado"
        );

        atualizarBotoes();

        if (documento.isBlank()) {
            mensagemErro.setText(
                    "Informe o CPF ou a matrícula do usuário."
            );
            return;
        }

        botaoIdentificar.setDisable(true);
        botaoVerificar.setDisable(true);
        campoIdentificacao.setDisable(true);
        comboItem.setDisable(true);

        Task<Usuario> tarefa = new Task<>() {
            @Override
            protected Usuario call() {
                return App.servicos()
                        .usuarios()
                        .identificar(documento);
            }
        };

        tarefa.setOnSucceeded(evento -> {
            campoIdentificacao.setDisable(false);
            comboItem.setDisable(false);
            botaoVerificar.setDisable(false);

            usuarioIdentificado = tarefa.getValue();

            // CU 9 passo 05: exibe os dados do Usuário encontrado
            rotuloUsuario.setText(
                    DadosUsuario.formatar(usuarioIdentificado)
            );

            atualizarBotoes();
        });

        tarefa.setOnFailed(evento -> {
            campoIdentificacao.setDisable(false);
            comboItem.setDisable(false);
            botaoVerificar.setDisable(false);

            Throwable causa = tarefa.getException();

            if (causa instanceof RegraNegocioException) {
                mensagemErro.setText(causa.getMessage());
            } else {
                Alertas.erro(
                        "Não foi possível identificar o usuário.",
                        causa
                );
            }

            atualizarBotoes();
        });

        iniciar(tarefa, "identificar-usuario-reserva");
    }

    private void atualizarBotoes() {
        boolean itemVerificado =
                itemIndisponivel != null;

        campoIdentificacao.setDisable(
                !itemVerificado || processando
        );

        botaoIdentificar.setDisable(
                !itemVerificado || processando
        );

        botaoConfirmar.setDisable(
                processando || !itemVerificado
                        || usuarioIdentificado == null
        );
    }

    // CU 11, passos 05 e 06; fluxo alternativo 5.1.
    @FXML
    private void onConfirmar() {
        Item item = itemIndisponivel;
        Usuario usuario = usuarioIdentificado;

        if (item == null || usuario == null) {
            mensagemErro.setText(
                    "Verifique o item e identifique o usuário "
                            + "antes de reservar."
            );
            return;
        }

        boolean confirmou = Alertas.confirmar(
                "Registrar reserva",
                "Usuário: " + usuario.getNome()
                        + "\nObra: " + item.getTitulo()
                        + "\n\nConfirmar reserva?"
        );

        if (!confirmou) {
            return;
        }

        Usuario administrador =
                Sessao.getAdministrador();

        mensagemErro.setText("");
        processando = true;

        botaoConfirmar.setDisable(true);
        botaoIdentificar.setDisable(true);
        botaoVerificar.setDisable(true);
        campoIdentificacao.setDisable(true);
        comboItem.setDisable(true);

        Task<Reserva> tarefa = new Task<>() {
            @Override
            protected Reserva call() {
                // O serviço também verifica disponibilidade
                // e duplicidade antes de registrar.
                return App.servicos()
                        .reservas()
                        .registrar(
                                usuario,
                                item,
                                administrador
                        );
            }
        };

        tarefa.setOnSucceeded(evento -> {
            processando = false;
            Reserva reserva = tarefa.getValue();

            Alertas.sucesso(
                    "Reserva registrada",
                    "Posição na fila: "
                            + reserva.getPosicaoFila()
                            + "\nValidade: "
                            + Datas.formatar(
                                    reserva.getValidadeMaxima()
                            )
            );

            mostrarConsulta(true);
            onPesquisar();
        });

        tarefa.setOnFailed(evento -> {
            processando = false;
            comboItem.setDisable(false);
            botaoVerificar.setDisable(false);

            Throwable causa = tarefa.getException();

            if (causa instanceof RegraNegocioException) {
                // Item disponível ou reserva duplicada:
                // mostra a mensagem retornada pelo serviço.
                mensagemErro.setText(causa.getMessage());

                if (causa.getMessage() != null
                        && causa.getMessage().startsWith("O item possui exemplar")) {
                    itemIndisponivel = null;
                    rotuloDisponibilidade.setText("A disponibilidade mudou. Verifique o item novamente.");
                }

                atualizarBotoes();
            } else {
                Alertas.erro(
                        "Não foi possível registrar a reserva.",
                        causa
                );

                atualizarBotoes();
            }
        });

        iniciar(tarefa, "registrar-reserva");
    }

    private void configurarConsulta() {
        comboStatus.getItems().add(null);
        comboStatus.getItems().addAll(
                StatusReserva.values()
        );
        comboStatus.setPromptText("Todos");

        comboOrdem.getItems().setAll(
                "Crescente",
                "Decrescente"
        );
        comboOrdem.setValue("Crescente");

        colunaPosicao.setCellValueFactory(dados ->
                texto(
                        dados.getValue()
                                .getRotuloPosicao()
                )
        );

        colunaLeitor.setCellValueFactory(dados ->
                texto(
                        dados.getValue()
                                .getUsuario()
                                .getNome()
                )
        );

        colunaObra.setCellValueFactory(dados ->
                texto(
                        dados.getValue()
                                .getItem()
                                .getTitulo()
                )
        );

        colunaData.setCellValueFactory(dados ->
                texto(
                        Datas.formatar(
                                dados.getValue()
                                        .getDataReserva()
                        )
                )
        );

        colunaValidade.setCellValueFactory(dados ->
                texto(
                        Datas.formatar(
                                dados.getValue()
                                        .getValidadeMaxima()
                        )
                )
        );

        colunaStatus.setCellValueFactory(dados ->
                texto(
                        dados.getValue()
                                .getStatus()
                                .getRotulo()
                )
        );

        colunaAcoes.setCellValueFactory(dados ->
                texto("acao")
        );

        colunaAcoes.setCellFactory(coluna -> new TableCell<>() {

            private final Button botao = new Button("◉");

            {
                botao.getStyleClass().add(
                        "botao-acao-reserva"
                );

                botao.setFocusTraversable(false);

                botao.setOnAction(evento -> {
                    Reserva reserva =
                            getTableRow().getItem();

                    if (reserva != null) {
                        Alertas.sucesso(
                                "Detalhes da reserva",
                                "Leitor: "
                                        + reserva.getUsuario().getNome()
                                        + "\nObra: "
                                        + reserva.getItem().getTitulo()
                                        + "\nPosição: "
                                        + reserva.getRotuloPosicao()
                                        + "\nValidade: "
                                        + Datas.formatar(
                                                reserva.getValidadeMaxima()
                                        )
                        );
                    }
                });
            }

            @Override
            protected void updateItem(
                    String item,
                    boolean vazio
            ) {
                super.updateItem(item, vazio);

                setGraphic(
                        vazio ? null : botao
                );

                setText(null);
            }
        });

        mostrarConsulta(false);
    }

    private ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(
                valor == null ? "" : valor
        );
    }

    private void mostrarConsulta(boolean consultar) {
        areaFormulario.setVisible(!consultar);
        areaFormulario.setManaged(!consultar);

        areaConsulta.setVisible(consultar);
        areaConsulta.setManaged(consultar);
    }

    @FXML
    private void onPesquisar() {
        if (botaoPesquisar.isDisable()) {
            return;
        }

        String termo = campoBusca.getText() == null
                ? ""
                : campoBusca.getText().trim();

        StatusReserva status =
                comboStatus.getValue();

        boolean crescente =
                !"Decrescente".equals(
                        comboOrdem.getValue()
                );

        botaoPesquisar.setDisable(true);

        tabelaReservas.setPlaceholder(
                new Label("Carregando reservas...")
        );

        Task<List<Reserva>> tarefa = new Task<>() {
            @Override
            protected List<Reserva> call() {
                return App.servicos()
                        .reservas()
                        .pesquisar(
                                termo,
                                status,
                                crescente
                        );
            }
        };

        tarefa.setOnSucceeded(evento -> {
            botaoPesquisar.setDisable(false);

            tabelaReservas.getItems().setAll(
                    tarefa.getValue()
            );

            tabelaReservas.setPlaceholder(
                    new Label(
                            "Nenhuma reserva encontrada."
                    )
            );

            mensagemConsulta.setText("");
        });

        tarefa.setOnFailed(evento -> {
            botaoPesquisar.setDisable(false);

            mensagemConsulta.setText(
                    "Não foi possível carregar as reservas."
            );

            Alertas.erro(
                    "Falha na consulta de reservas.",
                    tarefa.getException()
            );
        });

        iniciar(tarefa, "consultar-reservas");
    }

    @FXML
    private void onIrParaEmprestimo() {
        Navegador.irPara(
                Navegador.Tela.EMPRESTIMO
        );
    }

    @FXML
    private void onVoltar() {
        if (areaConsulta.isVisible()) {
            mostrarConsulta(false);
        } else {
            Navegador.irPara(
                    Navegador.Tela.VISAO_GERAL
            );
        }
    }

    private void iniciar(Task<?> tarefa, String nome) {
        Thread thread = new Thread(tarefa, nome);
        thread.setDaemon(true);
        thread.start();
    }
    @FXML
private void onConsultarReservas() {
    mostrarConsulta(true);
    onPesquisar();
}
}
