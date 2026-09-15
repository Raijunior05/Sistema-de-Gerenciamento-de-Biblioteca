package br.univasf.bibliotech.exception;

/** CU 3 fluxo 4.1, CU 4 fluxo 4.1 e CU 7 fluxo 5.1. */
public class DadosDuplicadosException extends RegraNegocioException {

    private final String campo;

    public DadosDuplicadosException(String campo, String valor) {
        super("Ja existe um registro com o " + campo + " " + valor + ".");
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
