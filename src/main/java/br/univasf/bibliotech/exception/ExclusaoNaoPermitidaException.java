package br.univasf.bibliotech.exception;

/** CU 5, fluxo alternativo 4.1. */
public class ExclusaoNaoPermitidaException extends RegraNegocioException {

    public ExclusaoNaoPermitidaException(String motivo) {
        super("Exclusao bloqueada: " + motivo);
    }
}
