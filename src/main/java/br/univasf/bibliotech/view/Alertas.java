package br.univasf.bibliotech.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/** Caixas de dialogo padronizadas. */
public final class Alertas {

    private Alertas() {
    }

    public static void erro(String cabecalho, Throwable causa) {
        erro(cabecalho, causa == null ? "" : String.valueOf(causa.getMessage()));
    }

    public static void erro(String cabecalho, String detalhe) {
        montar(Alert.AlertType.ERROR, "Erro", cabecalho, detalhe).showAndWait();
    }

    public static void aviso(String cabecalho, String detalhe) {
        montar(Alert.AlertType.WARNING, "Atencao", cabecalho, detalhe).showAndWait();
    }

    public static void sucesso(String cabecalho, String detalhe) {
        montar(Alert.AlertType.INFORMATION, "Pronto", cabecalho, detalhe).showAndWait();
    }

    /** @return verdadeiro quando o usuario confirma a acao */
    public static boolean confirmar(String cabecalho, String detalhe) {
        Optional<ButtonType> escolha =
                montar(Alert.AlertType.CONFIRMATION, "Confirmacao", cabecalho, detalhe)
                        .showAndWait();
        return escolha.isPresent() && escolha.get() == ButtonType.OK;
    }

    private static Alert montar(Alert.AlertType tipo, String titulo,
                                String cabecalho, String detalhe) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(cabecalho);
        a.setContentText(detalhe);
        return a;
    }
}
