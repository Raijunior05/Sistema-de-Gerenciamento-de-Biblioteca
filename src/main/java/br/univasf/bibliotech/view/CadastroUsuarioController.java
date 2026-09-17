package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.exception.DadosDuplicadosException;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Perfil;
import br.univasf.bibliotech.model.Usuario;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import java.util.Arrays;

/**
 * Controller do Caso de Uso 3 - Cadastrar Usuário.
 */
public class CadastroUsuarioController {

    @FXML private TextField campoNome;
    @FXML private TextField campoEmail;
    @FXML private RadioButton radioCpf;
    @FXML private RadioButton radioMatricula;
    @FXML private TextField campoDocumento;
    @FXML private TextField campoTelefone;
    @FXML private DatePicker campoNascimento;
    @FXML private ComboBox<Perfil> comboPerfil;

    @FXML private javafx.scene.layout.VBox areaAdmin;
    @FXML private TextField campoLogin;
    @FXML private PasswordField campoSenha;
    @FXML private PasswordField campoConfirmaSenha;

    @FXML private Label mensagemErro;
    @FXML private Button botaoSalvar;
    @FXML private Label tituloTela;

    @FXML
    private void initialize() {
        comboPerfil.getItems().setAll(Perfil.values());

        // Ouve a seleção do Perfil no ComboBox
        comboPerfil.valueProperty().addListener((obs, antigo, novoPerfil) -> {
            boolean isAdmin = novoPerfil == Perfil.ADMINISTRADOR;
            atualizarEstadoFormulario(isAdmin);
        });

        // Define Usuário como estado inicial padrão
        comboPerfil.setValue(Perfil.USUARIO);
        atualizarEstadoFormulario(false);
    }

    private void atualizarEstadoFormulario(boolean isAdmin) {
        areaAdmin.setVisible(isAdmin);
        areaAdmin.setManaged(isAdmin);

        if (!isAdmin) {
            campoLogin.clear();
            campoSenha.clear();
            campoConfirmaSenha.clear();
        }

        if (tituloTela != null) {
            tituloTela.setText(isAdmin ? "CADASTRAR  ADMIN" : "C A D A S T R O");
        }
    }

    @FXML
    private void onSalvar() {
        mensagemErro.setText("");

        Usuario usuario = new Usuario();
        usuario.setNome(campoNome.getText() != null ? campoNome.getText().trim() : "");
        usuario.setEmail(campoEmail.getText() != null ? campoEmail.getText().trim() : "");

        // Atribui o documento conforme o RadioButton selecionado (CPF ou Matrícula)
        String doc = campoDocumento.getText() != null ? campoDocumento.getText().trim() : "";
        if (radioCpf.isSelected()) {
            usuario.setCpf(doc);
        } else if (radioMatricula.isSelected()) {
            usuario.setMatricula(doc);
        }

        usuario.setTelefone(campoTelefone != null && campoTelefone.getText() != null ? campoTelefone.getText().trim() : "");
        usuario.setNascimento(campoNascimento != null ? campoNascimento.getValue() : null);
        usuario.setPerfil(comboPerfil.getValue());

        char[] senha = null;

        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {
            usuario.setLogin(campoLogin.getText() != null ? campoLogin.getText().trim() : "");

            senha = campoSenha.getText() != null ? campoSenha.getText().toCharArray() : new char[0];
            char[] confirmaSenha = campoConfirmaSenha.getText() != null
                    ? campoConfirmaSenha.getText().toCharArray()
                    : new char[0];

            if (senha.length == 0) {
                mensagemErro.setText("A senha é obrigatória para administradores.");
                return;
            }

            if (!Arrays.equals(senha, confirmaSenha)) {
                mensagemErro.setText("A confirmação de senha não confere com a senha digitada.");
                Arrays.fill(senha, ' ');
                Arrays.fill(confirmaSenha, ' ');
                return;
            }
            Arrays.fill(confirmaSenha, ' ');
        }

        final char[] senhaFinal = senha;
        botaoSalvar.setDisable(true);

        Task<Void> tarefa = new Task<>() {
            @Override
            protected Void call() {
                App.servicos().usuarios().cadastrar(usuario, senhaFinal);
                return null;
            }
        };

        tarefa.setOnSucceeded(e -> {
            if (senhaFinal != null) Arrays.fill(senhaFinal, ' ');
            botaoSalvar.setDisable(false);
            Alertas.sucesso("Usuário Cadastrado", "O cadastro do usuário foi realizado com sucesso!");
            Navegador.irPara(Navegador.Tela.VISAO_GERAL);
        });

        tarefa.setOnFailed(e -> {
            if (senhaFinal != null) Arrays.fill(senhaFinal, ' ');
            botaoSalvar.setDisable(false);

            Throwable causa = tarefa.getException();
            if (causa instanceof DadosDuplicadosException || causa instanceof RegraNegocioException) {
                mensagemErro.setText(causa.getMessage());
            } else {
                Alertas.erro("Não foi possível salvar o usuário.", causa);
            }
        });

        Thread thread = new Thread(tarefa, "cadastrar-usuario");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onCancelar() {
        Navegador.irPara(Navegador.Tela.VISAO_GERAL);
    }
}