package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.util.Sessao;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Caso de Uso 1 - Validar Usuario.
 *
 * <p>Serve de modelo para os demais controllers: nenhuma chamada ao banco
 * acontece na JavaFX Application Thread. A verificacao BCrypt leva cerca de
 * 250 ms e congelaria a janela se fosse executada diretamente no
 * {@code onAction}.</p>
 */
public class LoginController {

    @FXML private TextField campoEmail;
    @FXML private PasswordField campoSenha;
    @FXML private Label mensagem;
    @FXML private Button botaoEntrar;

    @FXML
    private void onEntrar() {
        String credencial = campoEmail.getText();
        char[] senha = campoSenha.getText().toCharArray();

        mensagem.setText("");
        botaoEntrar.setDisable(true);

        Task<Usuario> tarefa = new Task<>() {
            @Override
            protected Usuario call() {
                return App.servicos().autenticacao().autenticar(credencial, senha);
            }
        };

        tarefa.setOnSucceeded(e -> {
            botaoEntrar.setDisable(false);
            Sessao.iniciar(tarefa.getValue());
            Navegador.irPara(Navegador.Tela.VISAO_GERAL);
        });

        tarefa.setOnFailed(e -> {
            botaoEntrar.setDisable(false);
            campoSenha.clear();

            Throwable causa = tarefa.getException();
            if (causa instanceof RegraNegocioException) {
                // Fluxo alternativo 3.1 - mensagem inline, sem popup.
                mensagem.setText(causa.getMessage());
            } else {
                Alertas.erro("Nao foi possivel acessar o sistema.", causa);
            }
        });

        Thread thread = new Thread(tarefa, "login");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onCancelar() {
        campoEmail.clear();
        campoSenha.clear();
        mensagem.setText("");
    }

    @FXML
    private void onEsqueciSenha() {
        Alertas.aviso("Recuperacao de senha",
                "O sistema opera offline (RNF-02), sem envio de e-mail. "
                        + "Solicite a outro administrador que redefina sua senha "
                        + "pela tela Administrar usuarios.");
    }
}
