package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.util.Sessao;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class ConsultaUsuarioController {

    @FXML
    private TextField campoNome;

    @FXML
    private TableView<Usuario> tabelaUsuarios;

    @FXML
    private TableColumn<Usuario, String> colunaNome;

    @FXML
    private TableColumn<Usuario, String> colunaEmail;

    @FXML
    private TableColumn<Usuario, String> colunaIdentificacao;

    @FXML
    private TableColumn<Usuario, String> colunaPerfil;

    @FXML
    private TableColumn<Usuario, String> colunaStatus;

    @FXML
    private Label rodapeNome;

    @FXML
    private void initialize() {

        if (Sessao.estaAutenticado()) {
            rodapeNome.setText(
                    Sessao.getAdministrador().getNome()
            );
        }

        colunaNome.setCellValueFactory(dados ->
                new SimpleStringProperty(
                        valor(dados.getValue().getNome())
                )
        );

        colunaEmail.setCellValueFactory(dados ->
                new SimpleStringProperty(
                        valor(dados.getValue().getEmail())
                )
        );

        colunaIdentificacao.setCellValueFactory(dados ->
                new SimpleStringProperty(
                        valor(dados.getValue().getIdentificacao())
                )
        );

        colunaPerfil.setCellValueFactory(dados ->
                new SimpleStringProperty(
                        dados.getValue().getPerfil() != null
                                ? dados.getValue().getPerfil().toString()
                                : ""
                )
        );

        colunaStatus.setCellValueFactory(dados ->
                new SimpleStringProperty(
                        dados.getValue().isAtivo()
                                ? "Ativo"
                                : "Inativo"
                )
        );

        // Ao abrir a tela consulta TODOS os usuários do banco
        carregarUsuarios("");
    }

    private void carregarUsuarios(String termo) {

        tabelaUsuarios.setPlaceholder(
                new Label("Carregando usuários...")
        );

        Task<List<Usuario>> tarefa = new Task<>() {

            @Override
            protected List<Usuario> call() {

                if (termo == null || termo.isBlank()) {

                    // SELECT de todos os usuários
                    return App.servicos()
                            .usuarios()
                            .listarTodos();

                }

                // Pesquisa usuário pelo termo digitado
                return App.servicos()
                        .usuarios()
                        .pesquisar(termo.trim());
            }
        };

        tarefa.setOnSucceeded(evento -> {

            List<Usuario> usuarios = tarefa.getValue();

            tabelaUsuarios.getItems().setAll(usuarios);

            if (usuarios.isEmpty()) {
                tabelaUsuarios.setPlaceholder(
                        new Label("Nenhum usuário encontrado.")
                );
            }
        });

        tarefa.setOnFailed(evento -> {

            tabelaUsuarios.getItems().clear();

            tabelaUsuarios.setPlaceholder(
                    new Label("Erro ao consultar usuários.")
            );

            Alertas.erro(
                    "Não foi possível consultar os usuários.",
                    tarefa.getException()
            );
        });

        Thread thread = new Thread(
                tarefa,
                "consultar-usuarios"
        );

        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onPesquisar() {

        String nome = campoNome.getText();

        carregarUsuarios(nome);
    }

    @FXML
    private void onVoltar() {

        Navegador.irPara(
                Navegador.Tela.USUARIOS
        );
    }

    @FXML
    private void onSair() {

        if (Alertas.confirmar(
                "Encerrar sessão",
                "Deseja sair do sistema?"
        )) {

            Sessao.encerrar();

            Navegador.irPara(
                    Navegador.Tela.LOGIN
            );
        }
    }

    private String valor(String texto) {

        return texto == null
                ? ""
                : texto;
    }
}