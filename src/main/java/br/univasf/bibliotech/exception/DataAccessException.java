package br.univasf.bibliotech.exception;

/** Falha tecnica no acesso ao banco. Envolve SQLException. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public DataAccessException(String mensagem) {
        super(mensagem);
    }
}
