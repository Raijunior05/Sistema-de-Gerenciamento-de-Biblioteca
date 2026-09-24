package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.util.Sessao;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class ConsultaUsuarioController {

    /*
     * ============================================================
     * CAMPOS DA TELA
     * ============================================================
     */

    @FXML
    private TextField campoNome;

    @FXML
    private TableView<Usuario> tabelaUsuarios;

    @FXML
    private TableColumn<Usuario, String> colunaMatricula;

    @FXML
    private TableColumn<Usuario, String> colunaUsuario;

    @FXML
    private TableColumn<Usuario, String> colunaAcoes;

    @FXML
    private javafx.scene.control.Label rodapeNome;


    /*
     * ============================================================
     * INICIALIZAÇÃO
     * ============================================================
     */

    @FXML
    private void initialize() {

        /*
         * Nome do administrador no rodapé
         */
        if (Sessao.estaAutenticado()) {

            rodapeNome.setText(
                    Sessao.getAdministrador().getNome()
            );
        }


        /*
         * Configura as colunas da tabela
         */
        configurarTabela();


        /*
         * A tabela começa escondida
         */
        tabelaUsuarios.setVisible(false);
        tabelaUsuarios.setManaged(false);
    }


    /*
     * ============================================================
     * CONFIGURAÇÃO DA TABELA
     * ============================================================
     */

    private void configurarTabela() {

        /*
         * MATRÍCULA / CPF
         */
        colunaMatricula.setCellValueFactory(dados -> {

            Usuario usuario = dados.getValue();

            String identificacao =
                    usuario.getMatricula();

            if (identificacao == null
                    || identificacao.isBlank()) {

                identificacao =
                        usuario.getCpf();
            }

            return new SimpleStringProperty(
                    identificacao != null
                            ? identificacao
                            : ""
            );
        });


        /*
         * NOME DO USUÁRIO
         */
        colunaUsuario.setCellValueFactory(dados -> {

            Usuario usuario =
                    dados.getValue();

            return new SimpleStringProperty(
                    usuario.getNome() != null
                            ? usuario.getNome()
                            : ""
            );
        });


        /*
         * Necessário para a coluna AÇÕES
         */
        colunaAcoes.setCellValueFactory(dados ->
                new SimpleStringProperty("acao")
        );


        /*
         * ========================================================
         * BOTÃO DA COLUNA AÇÕES
         * ========================================================
         */
        colunaAcoes.setCellFactory(coluna ->

                new TableCell<>() {

                    private final Button botao =
                            new Button("◉");


                    /*
                     * Bloco de inicialização do botão
                     */
                    {
                        botao.getStyleClass()
                                .add(
                                        "botao-acao-consulta"
                                );

                        botao.setFocusTraversable(
                                false
                        );


                        botao.setOnAction(evento -> {

                            int indice =
                                    getIndex();


                            if (indice < 0
                                    || indice >= getTableView()
                                    .getItems()
                                    .size()) {

                                return;
                            }


                            Usuario usuario =
                                    getTableView()
                                            .getItems()
                                            .get(indice);


                            mostrarUsuario(
                                    usuario
                            );
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

                            setText(null);

                        } else {

                            setGraphic(botao);

                            setText(null);
                        }
                    }
                }
        );
    }


    /*
     * ============================================================
     * PESQUISAR
     * ============================================================
     */

    @FXML
    private void onPesquisar() {

        String termo =
                campoNome.getText() != null
                        ? campoNome
                        .getText()
                        .trim()
                        : "";


        /*
         * Mostra a tabela
         */
        tabelaUsuarios.setVisible(true);
        tabelaUsuarios.setManaged(true);


        /*
         * ========================================================
         * BUSCA EM SEGUNDO PLANO
         * ========================================================
         */

        Task<List<Usuario>> tarefa =
                new Task<>() {

                    @Override
                    protected List<Usuario> call() {

                        /*
                         * Se não digitou nada,
                         * mostra TODOS.
                         */
                        if (termo.isBlank()) {

                            return App.servicos()
                                    .usuarios()
                                    .listarTodos();
                        }


                        /*
                         * Se digitou algo,
                         * pesquisa.
                         */
                        return App.servicos()
                                .usuarios()
                                .pesquisar(
                                        termo
                                );
                    }
                };


        /*
         * ========================================================
         * SUCESSO
         * ========================================================
         */

        tarefa.setOnSucceeded(evento -> {

            List<Usuario> usuarios =
                    tarefa.getValue();


            tabelaUsuarios
                    .getItems()
                    .clear();


            if (usuarios != null) {

                tabelaUsuarios
                        .getItems()
                        .addAll(
                                usuarios
                        );
            }


            /*
             * Caso não tenha usuários
             */
            if (usuarios == null
                    || usuarios.isEmpty()) {

                tabelaUsuarios.setPlaceholder(
                        new javafx.scene.control.Label(
                                "Nenhum usuário encontrado."
                        )
                );
            }
        });


        /*
         * ========================================================
         * ERRO
         * ========================================================
         */

        tarefa.setOnFailed(evento -> {

            tabelaUsuarios
                    .getItems()
                    .clear();


            Alertas.erro(
                    "Não foi possível consultar os usuários.",
                    tarefa.getException()
            );
        });


        /*
         * Executa fora da thread gráfica
         */
        Thread thread =
                new Thread(
                        tarefa,
                        "consultar-usuarios"
                );


        thread.setDaemon(true);

        thread.start();
    }


    /*
     * ============================================================
     * MOSTRAR DADOS DO USUÁRIO
     * ============================================================
     */

    private void mostrarUsuario(
            Usuario usuario
    ) {

        String identificacao =
                usuario.getMatricula();


        if (identificacao == null
                || identificacao.isBlank()) {

            identificacao =
                    usuario.getCpf();
        }


        String perfil =
                usuario.getPerfil() != null
                        ? usuario.getPerfil().toString()
                        : "";


        String email =
                usuario.getEmail() != null
                        ? usuario.getEmail()
                        : "";


        Alert alerta =
                new Alert(
                        Alert.AlertType.INFORMATION
                );


        alerta.setTitle(
                "Consultar usuário"
        );


        alerta.setHeaderText(
                usuario.getNome()
        );


        alerta.setContentText(
                "Identificação: "
                        + (identificacao != null
                        ? identificacao
                        : "")
                        + "\n"
                        + "E-mail: "
                        + email
                        + "\n"
                        + "Perfil: "
                        + perfil
        );


        alerta.showAndWait();
    }


    /*
     * ============================================================
     * VOLTAR
     * ============================================================
     */

    @FXML
    private void onVoltar() {

        Navegador.irPara(
                Navegador.Tela.USUARIOS
        );
    }


    /*
     * ============================================================
     * SAIR
     * ============================================================
     */

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