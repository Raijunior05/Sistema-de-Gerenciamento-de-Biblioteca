package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.exception.DadosDuplicadosException;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Perfil;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.util.Sessao;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;

public class EditarUsuarioController {

    /*
     * ============================================================
     * ÁREA DE BUSCA
     * ============================================================
     */

    @FXML
    private VBox areaBusca;

    @FXML
    private TextField campoBusca;

    @FXML
    private TableView<Usuario> tabelaUsuarios;

    @FXML
    private TableColumn<Usuario, String> colunaMatricula;

    @FXML
    private TableColumn<Usuario, String> colunaUsuario;

    @FXML
    private TableColumn<Usuario, String> colunaAcoes;


    /*
     * ============================================================
     * ÁREA DO FORMULÁRIO
     * ============================================================
     */

    @FXML
    private HBox areaFormulario;

    @FXML
    private Label tituloFormulario;

    @FXML
    private TextField campoNome;

    @FXML
    private TextField campoEmail;

    @FXML
    private RadioButton radioCpf;

    @FXML
    private RadioButton radioMatricula;

    @FXML
    private TextField campoDocumento;

    @FXML
    private ComboBox<Perfil> comboPerfil;


    /*
     * CAMPOS DE ADMINISTRADOR
     */

    @FXML
    private VBox areaAdmin;

    @FXML
    private TextField campoLogin;

    @FXML
    private PasswordField campoSenha;

    @FXML
    private PasswordField campoConfirmaSenha;


    /*
     * CONTROLES GERAIS
     */

    @FXML
    private Label mensagemErro;

    @FXML
    private Button botaoSalvar;

    @FXML
    private Label rodapeNome;


    /*
     * USUÁRIO SELECIONADO
     */

    private Usuario usuarioSelecionado;


    @FXML
    private void initialize() {

        if (Sessao.estaAutenticado()) {
            rodapeNome.setText(
                    Sessao.getAdministrador().getNome()
            );
        }

        comboPerfil.getItems().setAll(
                Perfil.values()
        );

        comboPerfil.valueProperty().addListener((obs, antigo, novoPerfil) -> {
            atualizarAreaAdministrador(
                    novoPerfil == Perfil.ADMINISTRADOR
            );
        });

        configurarTabela();

        mostrarTelaBusca();
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

        colunaUsuario.setCellValueFactory(dados ->
                new SimpleStringProperty(
                        valor(dados.getValue().getNome())
                )
        );

        colunaAcoes.setCellFactory(coluna ->
                new TableCell<>() {

                    private final Button botao = new Button("◉");

                    {
                        botao.getStyleClass().add("botao-acao-consulta");
                        botao.setFocusTraversable(false);

                        botao.setOnAction(evento -> {
                            Usuario usuario = getTableView()
                                    .getItems()
                                    .get(getIndex());

                            carregarUsuario(usuario);
                        });
                    }

                    @Override
                    protected void updateItem(String item, boolean vazio) {
                        super.updateItem(item, vazio);

                        if (vazio) {
                            setGraphic(null);
                        } else {
                            setGraphic(botao);
                        }
                    }
                }
        );
    }


    /*
     * ============================================================
     * LUPA - MOSTRA TODOS OS USUÁRIOS
     * ============================================================
     */

    @FXML
    private void onPesquisar() {

        tabelaUsuarios.setVisible(true);
        tabelaUsuarios.setManaged(true);

        tabelaUsuarios.setPlaceholder(
                new Label("Carregando usuários...")
        );

        String termo = campoBusca.getText() != null
                ? campoBusca.getText().trim()
                : "";

        Task<List<Usuario>> tarefa = new Task<>() {
            @Override
            protected List<Usuario> call() {

                if (termo.isBlank()) {
                    return App.servicos()
                            .usuarios()
                            .listarTodos();
                }

                return App.servicos()
                        .usuarios()
                        .pesquisar(termo);
            }
        };

        tarefa.setOnSucceeded(evento -> {

            List<Usuario> usuarios = tarefa.getValue();

            tabelaUsuarios.getItems().setAll(usuarios);

            if (usuarios == null || usuarios.isEmpty()) {
                tabelaUsuarios.setPlaceholder(
                        new Label("Nenhum usuário encontrado.")
                );
            }
        });

        tarefa.setOnFailed(evento -> {

            tabelaUsuarios.getItems().clear();

            Alertas.erro(
                    "Não foi possível carregar os usuários.",
                    tarefa.getException()
            );
        });

        Thread thread = new Thread(
                tarefa,
                "listar-usuarios-edicao"
        );

        thread.setDaemon(true);
        thread.start();
    }


    /*
     * ============================================================
     * CARREGA O USUÁRIO NO FORMULÁRIO
     * ============================================================
     */

    private void carregarUsuario(Usuario usuario) {

        usuarioSelecionado = usuario;

        mensagemErro.setText("");

        campoNome.setText(
                valor(usuario.getNome())
        );

        campoEmail.setText(
                valor(usuario.getEmail())
        );

        if (usuario.getMatricula() != null
                && !usuario.getMatricula().isBlank()) {

            radioMatricula.setSelected(true);
            campoDocumento.setText(
                    usuario.getMatricula()
            );

        } else {

            radioCpf.setSelected(true);
            campoDocumento.setText(
                    valor(usuario.getCpf())
            );
        }

        comboPerfil.setValue(
                usuario.getPerfil()
        );

        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {

            tituloFormulario.setText(
                    "EDITAR ADMINISTRADOR"
            );

            campoLogin.setText(
                    valor(usuario.getLogin())
            );

            atualizarAreaAdministrador(true);

        } else {

            tituloFormulario.setText(
                    "EDITAR USUÁRIO"
            );

            campoLogin.clear();

            atualizarAreaAdministrador(false);
        }

        campoSenha.clear();
        campoConfirmaSenha.clear();

        areaBusca.setVisible(false);
        areaBusca.setManaged(false);

        areaFormulario.setVisible(true);
        areaFormulario.setManaged(true);
    }


    private void atualizarAreaAdministrador(boolean administrador) {

        areaAdmin.setVisible(administrador);
        areaAdmin.setManaged(administrador);

        if (tituloFormulario != null) {
            tituloFormulario.setText(
                    administrador
                            ? "EDITAR ADMINISTRADOR"
                            : "EDITAR USUÁRIO"
            );
        }

        if (!administrador) {
            campoLogin.clear();
            campoSenha.clear();
            campoConfirmaSenha.clear();
        }
    }


    /*
     * ============================================================
     * SALVAR
     * ============================================================
     */

    @FXML
    private void onSalvar() {

        mensagemErro.setText("");

        if (usuarioSelecionado == null) {
            mensagemErro.setText(
                    "Nenhum usuário foi selecionado."
            );
            return;
        }

        if (comboPerfil.getValue() == null) {
            mensagemErro.setText(
                    "Selecione o perfil de acesso."
            );
            return;
        }

        Usuario usuario = new Usuario();

        usuario.setId(
                usuarioSelecionado.getId()
        );

        usuario.setNome(
                texto(campoNome)
        );

        usuario.setEmail(
                texto(campoEmail)
        );

        String documento = texto(campoDocumento);

        if (radioCpf.isSelected()) {
            usuario.setCpf(documento);
            usuario.setMatricula(null);

        } else if (radioMatricula.isSelected()) {
            usuario.setMatricula(documento);
            usuario.setCpf(null);

        } else {
            mensagemErro.setText(
                    "Selecione CPF ou Matrícula."
            );
            return;
        }

        usuario.setPerfil(
                comboPerfil.getValue()
        );

        usuario.setTelefone(
                usuarioSelecionado.getTelefone()
        );

        usuario.setNascimento(
                usuarioSelecionado.getNascimento()
        );

        usuario.setAtivo(
                usuarioSelecionado.isAtivo()
        );

        usuario.setCriadoEm(
                usuarioSelecionado.getCriadoEm()
        );

        usuario.setSenhaHash(
                usuarioSelecionado.getSenhaHash()
        );

        char[] novaSenha = null;

        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {

            String login = texto(campoLogin);

            if (login.isBlank()) {
                mensagemErro.setText(
                        "Informe o nome de usuário do administrador."
                );
                return;
            }

            usuario.setLogin(login);

            char[] senha = campoSenha.getText() != null
                    ? campoSenha.getText().toCharArray()
                    : new char[0];

            char[] confirmar = campoConfirmaSenha.getText() != null
                    ? campoConfirmaSenha.getText().toCharArray()
                    : new char[0];

            boolean virouAdministrador =
                    usuarioSelecionado.getPerfil() != Perfil.ADMINISTRADOR;

            if (virouAdministrador && senha.length == 0) {
                mensagemErro.setText(
                        "Informe uma senha para o novo administrador."
                );
                Arrays.fill(confirmar, ' ');
                return;
            }

            if (senha.length > 0) {

                if (!Arrays.equals(senha, confirmar)) {

                    mensagemErro.setText(
                            "As senhas não conferem."
                    );

                    Arrays.fill(senha, ' ');
                    Arrays.fill(confirmar, ' ');
                    return;
                }

                novaSenha = senha;
            }

            Arrays.fill(confirmar, ' ');

        } else {

            usuario.setLogin(null);
            usuario.setSenhaHash(null);
        }

        final char[] senhaFinal = novaSenha;
        final long idAdministradorLogado = Sessao.getAdministrador().getId();

        botaoSalvar.setDisable(true);

        Task<Void> tarefa = new Task<>() {
            @Override
            protected Void call() {

                App.servicos()
                        .usuarios()
                        .editar(usuario, senhaFinal, idAdministradorLogado);

                return null;
            }
        };

        tarefa.setOnSucceeded(evento -> {

            if (senhaFinal != null) {
                Arrays.fill(senhaFinal, ' ');
            }

            botaoSalvar.setDisable(false);

            Alertas.sucesso(
                    "Usuário atualizado",
                    "Os dados do usuário foram atualizados com sucesso."
            );

            mostrarTelaBusca();
        });

        tarefa.setOnFailed(evento -> {

            if (senhaFinal != null) {
                Arrays.fill(senhaFinal, ' ');
            }

            botaoSalvar.setDisable(false);

            Throwable causa = tarefa.getException();

            if (causa instanceof DadosDuplicadosException
                    || causa instanceof RegraNegocioException) {

                mensagemErro.setText(
                        causa.getMessage()
                );

            } else {

                Alertas.erro(
                        "Não foi possível editar o usuário.",
                        causa
                );
            }
        });

        Thread thread = new Thread(
                tarefa,
                "editar-usuario"
        );

        thread.setDaemon(true);
        thread.start();
    }


    @FXML
    private void onCancelar() {
        mostrarTelaBusca();
    }


    @FXML
    private void onVoltar() {
        Navegador.irPara(
                Navegador.Tela.USUARIOS
        );
    }


    private void mostrarTelaBusca() {

        usuarioSelecionado = null;

        areaBusca.setVisible(true);
        areaBusca.setManaged(true);

        areaFormulario.setVisible(false);
        areaFormulario.setManaged(false);

        campoBusca.clear();

        tabelaUsuarios.setVisible(false);
        tabelaUsuarios.setManaged(false);
        tabelaUsuarios.getItems().clear();

        if (mensagemErro != null) {
            mensagemErro.setText("");
        }

        if (campoNome != null) {
            campoNome.clear();
            campoEmail.clear();
            campoDocumento.clear();
            campoLogin.clear();
            campoSenha.clear();
            campoConfirmaSenha.clear();
            comboPerfil.setValue(null);
        }
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


    private String texto(TextField campo) {
        return campo.getText() == null
                ? ""
                : campo.getText().trim();
    }

    private String valor(String valor) {
        return valor == null
                ? ""
                : valor;
    }
}