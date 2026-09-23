package br.univasf.bibliotech.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public final class Navegador {

    public enum Tela {
        LOGIN("/fxml/login.fxml", "BiblioTech - Acesso"),
        VISAO_GERAL("/fxml/visao-geral.fxml", "BiblioTech - Visao geral"),
        USUARIOS("/fxml/usuarios.fxml", "BiblioTech - Administrar usuarios"),
        CADASTRO_USUARIO("/fxml/cadastro-usuario.fxml", "BiblioTech - Cadastrar usuario"),
        CONSULTA_USUARIO("/fxml/consulta-usuario.fxml", "BiblioTech - Consultar usuario"),
        ACERVO("/fxml/acervo.fxml", "BiblioTech - Pesquisar acervo"),
        CADASTRO_ITEM("/fxml/cadastro-item.fxml", "BiblioTech - Cadastrar item"),
        EMPRESTIMO("/fxml/emprestimo.fxml", "BiblioTech - Realizar emprestimo"),
        DEVOLUCAO("/fxml/devolucao.fxml", "BiblioTech - Realizar devolucao"),
        RESERVA("/fxml/reserva.fxml", "BiblioTech - Realizar reserva"),
        EDITAR_USUARIO("/fxml/editar-usuario.fxml","BiblioTech - Editar usuario"),      
        RELATORIO("/fxml/relatorio.fxml", "BiblioTech - Relatorios");

        private final String caminho;
        private final String titulo;

        Tela(String caminho, String titulo) {
            this.caminho = caminho;
            this.titulo = titulo;
        }
    }

    private static Stage palco;

    private Navegador() {
    }

    public static void iniciar(Stage stage) {
        Font.loadFont(
                Navegador.class.getResourceAsStream("/fonts/Montserrat-Regular.ttf"),
                14
        );

        Font.loadFont(
                Navegador.class.getResourceAsStream("/fonts/Montserrat-Bold.ttf"),
                14
        );

        palco = stage;

        palco.setTitle("BiblioTech");
        palco.setMinWidth(960);
        palco.setMinHeight(580);
    }

    public static void irPara(Tela tela) {
        try {

            var recurso = Navegador.class.getResource(tela.caminho);

            if (recurso == null) {

                Alertas.aviso(
                        "Operação indisponível",
                        "Esta operação ainda não está disponível nesta versão."
                );

                return;
            }

            FXMLLoader loader = new FXMLLoader(recurso);

            Parent raiz = loader.load();

            Scene cena = palco.getScene();

            if (cena == null) {

                cena = new Scene(
                        raiz,
                        960,
                        540
                );

                cena.getStylesheets().add(
                        Objects.requireNonNull(
                                Navegador.class.getResource("/css/sgb.css")
                        ).toExternalForm()
                );

                palco.setScene(cena);

            } else {

                cena.setRoot(raiz);
            }

            palco.setTitle(tela.titulo);

            if (!palco.isShowing()) {
                palco.show();
            }

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Falha ao carregar a tela " + tela,
                    e
            );
        }
    }

    public static Stage getPalco() {
        return palco;
    }
}