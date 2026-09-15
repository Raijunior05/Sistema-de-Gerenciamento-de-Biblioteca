package br.univasf.bibliotech.exception;

/** CU 9, fluxo alternativo 4.1. */
public class UsuarioNaoEncontradoException extends RegraNegocioException {

    private final String identificacao;

    public UsuarioNaoEncontradoException(String identificacao) {
        super("Nenhum usuario cadastrado com a identificacao " + identificacao + ".");
        this.identificacao = identificacao;
    }

    public String getIdentificacao() {
        return identificacao;
    }
}
