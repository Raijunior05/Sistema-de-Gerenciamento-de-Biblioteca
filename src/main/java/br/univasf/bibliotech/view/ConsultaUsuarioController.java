package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.util.Sessao;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

public class ConsultaUsuarioController {

    @FXML
    private TextField campoNome;

    @FXML
    private VBox painelConsulta;

    @FXML
    private VBox areaBusca;

    @FXML
    private TableView<Usuario> tabelaUsuarios;

    @FXML
    private TableColumn<Usuario, String> colunaMatricula;

    @FXML
    private TableColumn<Usuario, String> colunaUsuario;

    @FXML
    private TableColumn<Usuario, String> colunaAcoes;

    @FXML
    private Label rodapeNome;


    @FXML
    private void initialize() {

        if (Sessao.estaAutenticado()) {

            rodapeNome.setText(
                    Sessao.getAdministrador().getNome()
            );
        }


        colunaMatricula.setCellValueFactory(dados -> {

            String matricula =
                    dados.getValue().getMatricula();

            if (matricula == null ||
                    matricula.isBlank()) {

                matricula =
                        dados.getValue().getCpf();
            }

            return new SimpleStringProperty(
                    matricula != null
                            ? matricula
                            : ""
            );
        });


        colunaUsuario.setCellValueFactory(dados ->

                new SimpleStringProperty(
                        dados.getValue().getNome()
                )
        );


        configurarColunaAcoes();


        // Estado inicial igual ao protótipo
        tabelaUsuarios.setVisible(false);
        tabelaUsuarios.setManaged(false);
    }


    private void configurarColunaAcoes() {

        colunaAcoes.setCellFactory(coluna ->

                new TableCell<>() {

                    private final Button botao =
                            new Button("◉");


                    {
                        botao.getStyleClass()
                                .add("botao-acao-consulta");

                        botao.setOnAction(evento -> {

                            Usuario usuario =
                                    getTableView()
                                            .getItems()
                                            .get(getIndex());

                            mostrarUsuario(usuario);
                        });
                    }


                    @Override
                    protected void updateItem(
                            String item,
                            boolean vazio
                    ) {

                        super.updateItem(
                                item,
                                vazio
                        );

                        if (vazio) {

                            setGraphic(null);

                        } else {

                            setGraphic(botao);
                        }
                    }
                }
        );
    }


    @FXML
    private void onPesquisar() {

        String termo =
                campoNome.getText();

        carregarUsuarios(termo);
    }


    private void carregarUsuarios(
            String termo
    ) {

        tabelaUsuarios.setVisible(true);
        tabelaUsuarios.setManaged(true);

        /*
         * Depois da pesquisa o painel cresce,
         * como no segundo protótipo.
         */
        painelConsulta.setPrefHeight(410);

        tabelaUsuarios.setPlaceholder(
                new Label("Carregando usuários...")
        );


        Task<List<Usuario>> tarefa =
                new Task<>() {

            @Override
            protected List<Usuario> call() {

                /*
                 * Campo vazio:
                 * mostra TODOS os usuários.
                 */
                if (termo == null ||
                        termo.isBlank()) {

                    return App.servicos()
                            .usuarios()
                            .listarTodos();
                }


                /*
                 * Com nome digitado:
                 * pesquisa no banco.
                 */
                return App.servicos()
                        .usuarios()
                        .pesquisar(
                                termo.trim()
                        );
            }
        };


        tarefa.setOnSucceeded(evento -> {

            List<Usuario> usuarios =
                    tarefa.getValue();

            tabelaUsuarios
                    .getItems()
                    .setAll(usuarios);


            if (usuarios.isEmpty()) {

                tabelaUsuarios.setPlaceholder(
                        new Label(
                                "Nenhum usuário encontrado."
                        )
                );
            }
        });


        tarefa.setOnFailed(evento -> {

            tabelaUsuarios
                    .getItems()
                    .clear();

            Alertas.erro(
                    "Não foi possível consultar os usuários.",
                    tarefa.getException()
            );
        });


        Thread thread =
                new Thread(
                        tarefa,
                        "consultar-usuarios"
                );

        thread.setDaemon(true);

        thread.start();
    }


    private void mostrarUsuario(
            Usuario usuario
    ) {

        String identificacao =
                usuario.getMatricula();

        if (identificacao == null ||
                identificacao.isBlank()) {

            identificacao =
                    usuario.getCpf();
        }


        Alertas.aviso(
                "Dados do usuário",

                "Nome: "
                        + usuario.getNome()

                        + "\nE-mail: "
                        + usuario.getEmail()

                        + "\nMatrícula/CPF: "
                        + identificacao

                        + "\nPerfil: "
                        + usuario.getPerfil()
        );
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
}