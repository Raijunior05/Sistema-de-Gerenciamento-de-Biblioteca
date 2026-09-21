package br.univasf.bibliotech.view;

import br.univasf.bibliotech.util.Sessao;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Submenu de Gerenciar / Administrar Usuários. */
public class UsuariosController {

    @FXML private Label rodapeNome;

    @FXML
    private void initialize() {
        if (Sessao.estaAutenticado()) {
            rodapeNome.setText(Sessao.getAdministrador().getNome());
        }
    }

    @FXML
    private void onCadastrarUsuario() {
        Navegador.irPara(Navegador.Tela.CADASTRO_USUARIO);
    }

    @FXML
private void onConsultarUsuario() {

    Navegador.irPara(
            Navegador.Tela.CONSULTA_USUARIO
    );
}

    @FXML
    private void onEditarUsuario() {
        Alertas.aviso("Em desenvolvimento", "A tela de edição de usuários será aberta aqui.");
    }

    @FXML
    private void onExcluirUsuario() {
        Alertas.aviso("Em desenvolvimento", "A tela de exclusão de usuários será aberta aqui.");
    }

    @FXML
    private void onVoltar() {
        Navegador.irPara(Navegador.Tela.VISAO_GERAL);
    }

    @FXML
    private void onSair() {
        if (Alertas.confirmar("Encerrar sessão", "Deseja sair do sistema?")) {
            Sessao.encerrar();
            Navegador.irPara(Navegador.Tela.LOGIN);
        }
    }
}