package br.univasf.bibliotech.exception;

/** CU 1, fluxo alternativo 3.1. */
public class CredenciaisInvalidasException extends RegraNegocioException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha incorretos.");
    }
}
