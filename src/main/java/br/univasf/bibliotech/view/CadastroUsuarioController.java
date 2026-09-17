/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Arrays;

/**
 * Controller do Caso de Uso 3 - Cadastrar Usuário.
 */
public class CadastroUsuarioController {

    @FXML private TextField campoNome;
    @FXML private TextField campoEmail;
    @FXML private TextField campoCpf;
    @FXML private TextField campoMatricula;
    @FXML private TextField campoTelefone;
    @FXML private DatePicker campoNascimento;
    @FXML private ComboBox<Perfil> comboPerfil;

    @FXML private VBox areaAdmin;
    @FXML private TextField campoLogin;
    @FXML private PasswordField campoSenha;
    @FXML private PasswordField campoConfirmaSenha;

    @FXML private Label mensagemErro;
    @FXML private Button botaoSalvar;

    @FXML
    private void initialize() {
        comboPerfil.getItems().addAll(Perfil.values());
        comboPerfil.setValue(Perfil.USUARIO);

        // Controla exibição dos campos de login/senha dinamicamente de acordo com o Perfil
        areaAdmin.managedProperty().bind(areaAdmin.visibleProperty());
        comboPerfil.valueProperty().addListener((obs, antigo, novoPerfil) -> {
            boolean isAdmin = novoPerfil == Perfil.ADMINISTRADOR;
            areaAdmin.setVisible(isAdmin);
            if (!isAdmin) {
                campoLogin.clear();
                campoSenha.clear();
                campoConfirmaSenha.clear();
            }
        });
        areaAdmin.setVisible(false);
    }

    @FXML
    private void onSalvar() {
        mensagemErro.setText("");

        // 1. Extração dos dados informados
        Usuario usuario = new Usuario();
        usuario.setNome(campoNome.getText() != null ? campoNome.getText().trim() : "");
        usuario.setEmail(campoEmail.getText() != null ? campoEmail.getText().trim() : "");
        usuario.setCpf(campoCpf.getText() != null ? campoCpf.getText().trim() : "");
        usuario.setMatricula(campoMatricula.getText() != null ? campoMatricula.getText().trim() : "");
        usuario.setTelefone(campoTelefone.getText() != null ? campoTelefone.getText().trim() : "");
        usuario.setNascimento(campoNascimento.getValue());
        usuario.setPerfil(comboPerfil.getValue());

        char[] senha = null;

        // 2. Validações prévias da UI quando for perfil ADMINISTRADOR
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

        // 3. Execução assíncrona da gravação no banco via Service (CU 3)
        Task<Void> tarefa = new Task<>() {
            @Override
            protected Void call() {
                App.servicos().usuarios().cadastrar(usuario, senhaFinal);
                return null;
            }
        };

        tarefa.setOnSucceeded(e -> {
            if (senhaFinal != null) {
                Arrays.fill(senhaFinal, ' ');
            }
            botaoSalvar.setDisable(false);
            Alertas.sucesso("Usuário Cadastrado", "O cadastro do usuário foi realizado com sucesso!");
            Navegador.irPara(Navegador.Tela.VISAO_GERAL);
        });

        tarefa.setOnFailed(e -> {
            if (senhaFinal != null) {
                Arrays.fill(senhaFinal, ' ');
            }
            botaoSalvar.setDisable(false);

            Throwable causa = tarefa.getException();
            if (causa instanceof DadosDuplicadosException || causa instanceof RegraNegocioException) {
                // Apresenta erros de validação e duplicidade inline sem alterar os campos
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