package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.util.Sessao;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class ExcluirUsuarioController {

    @FXML
    private TextField campoBusca;

    @FXML
    private TableView<Usuario> tabelaUsuarios;

    @FXML
    private TableColumn<Usuario, String> colunaMatricula;

    @FXML
    private TableColumn<Usuario, String> colunaUsuario;

    @FXML
    private TableColumn<Usuario, Void> colunaAcoes;

    @FXML
    private Label rodapeNome;

    @FXML
    private void initialize() {
        if (Sessao.estaAutenticado()) {
            rodapeNome.setText(Sessao.getAdministrador().getNome());
        }

        configurarTabela();

        tabelaUsuarios.setVisible(false);
        tabelaUsuarios.setManaged(false);
    }

    private void configurarTabela() {
        colunaMatricula.setCellValueFactory(dados -> {
            Usuario usuario = dados.getValue();
            String identificacao = usuario.getMatricula();

            if (identificacao == null || identificacao.isBlank()) {
                identificacao = usuario.getCpf();
            }

            return new SimpleStringProperty(
                    identificacao != null ? identificacao : ""
            );
        });

        colunaUsuario.setCellValueFactory(dados -> {
            String nome = dados.getValue().getNome();

            return new SimpleStringProperty(
                    nome != null ? nome : ""
            );
        });

        colunaAcoes.setCellFactory(coluna -> new TableCell<>() {

            private final Button botaoExcluir = new Button("✎");

            {
                botaoExcluir.getStyleClass().add("botao-acao-consulta");
                botaoExcluir.setFocusTraversable(false);

                botaoExcluir.setOnAction(evento -> {
                    Usuario usuario = getTableRow().getItem();

                    if (usuario != null) {
                        confirmarExclusao(usuario);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean vazio) {
                super.updateItem(item, vazio);
                setGraphic(vazio ? null : botaoExcluir);
            }
        });
    }

    @FXML
    private void onPesquisar() {
        String termo = campoBusca.getText() != null
                ? campoBusca.getText().trim()
                : "";

        tabelaUsuarios.setVisible(true);
        tabelaUsuarios.setManaged(true);
        tabelaUsuarios.getItems().clear();
        tabelaUsuarios.setPlaceholder(new Label("Carregando usuários..."));

        Task<List<Usuario>> tarefa = new Task<>() {
            @Override
            protected List<Usuario> call() {
                if (termo.isBlank()) {
                    return App.servicos().usuarios().listarTodos();
                }

                return App.servicos().usuarios().pesquisar(termo);
            }
        };

        tarefa.setOnSucceeded(evento -> {
            tabelaUsuarios.getItems().setAll(tarefa.getValue());
            tabelaUsuarios.setPlaceholder(
                    new Label("Nenhum usuário encontrado.")
            );
        });

        tarefa.setOnFailed(evento -> {
            tabelaUsuarios.setPlaceholder(
                    new Label("Não foi possível carregar os usuários.")
            );

            Alertas.erro(
                    "Não foi possível carregar os usuários.",
                    tarefa.getException()
            );
        });

        iniciarEmSegundoPlano(tarefa, "pesquisar-usuario-exclusao");
    }

    private void confirmarExclusao(Usuario usuario) {
        boolean confirmou = Alertas.confirmar(
                "Excluir usuário",
                "Deseja realmente excluir o usuário "
                        + usuario.getNome() + "?"
        );

        if (confirmou) {
            excluirUsuario(usuario);
        }
    }

    private void excluirUsuario(Usuario usuario) {
        if (!Sessao.estaAutenticado()) {
            Alertas.aviso(
                    "Sessão inválida",
                    "Nenhum administrador está autenticado."
            );
            return;
        }

        long idAdministrador = Sessao.getAdministrador().getId();

        // Evita dois cliques de exclusão enquanto o banco processa a operação.
        tabelaUsuarios.setDisable(true);

        Task<Void> tarefa = new Task<>() {
            @Override
            protected Void call() {
                App.servicos().usuarios().excluir(
                        usuario.getId(),
                        idAdministrador
                );
                return null;
            }
        };

        tarefa.setOnSucceeded(evento -> {
            tabelaUsuarios.setDisable(false);
            tabelaUsuarios.getItems().remove(usuario);

            Alertas.sucesso(
                    "Usuário excluído",
                    "O usuário foi excluído com sucesso."
            );
        });

        tarefa.setOnFailed(evento -> {
            tabelaUsuarios.setDisable(false);

            Throwable causa = tarefa.getException();

            if (causa instanceof RegraNegocioException) {
                Alertas.aviso(
                        "Não foi possível excluir",
                        causa.getMessage()
                );
            } else {
                Alertas.erro(
                        "Não foi possível excluir o usuário.",
                        causa
                );
            }
        });

        iniciarEmSegundoPlano(tarefa, "excluir-usuario");
    }

    private void iniciarEmSegundoPlano(Task<?> tarefa, String nome) {
        Thread thread = new Thread(tarefa, nome);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onVoltar() {
        Navegador.irPara(Navegador.Tela.USUARIOS);
    }

    @FXML
    private void onSair() {
        if (Alertas.confirmar(
                "Encerrar sessão",
                "Deseja sair do sistema?"
        )) {
            Sessao.encerrar();
            Navegador.irPara(Navegador.Tela.LOGIN);
        }
    }
}