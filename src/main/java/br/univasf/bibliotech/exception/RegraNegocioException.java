package br.univasf.bibliotech.exception;

/**
 * Violacao de uma regra descrita na especificacao de casos de uso.
 * Cada subclasse corresponde a um fluxo alternativo do documento.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
