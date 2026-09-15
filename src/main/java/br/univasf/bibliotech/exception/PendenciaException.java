package br.univasf.bibliotech.exception;

/** CU 10, fluxo alternativo 7.1: itens em atraso. */
public class PendenciaException extends RegraNegocioException {

    public PendenciaException(String detalhe) {
        super("O usuario possui pendencias: " + detalhe);
    }
}
