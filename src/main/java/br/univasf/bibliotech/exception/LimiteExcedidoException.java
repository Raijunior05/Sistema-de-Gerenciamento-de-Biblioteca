package br.univasf.bibliotech.exception;

/** CU 10, fluxo alternativo 7.1. */
public class LimiteExcedidoException extends RegraNegocioException {

    private final int limite;
    private final int atuais;

    public LimiteExcedidoException(int limite, int atuais) {
        super("Limite de " + limite + " emprestimos atingido (o usuario possui "
                + atuais + " em aberto).");
        this.limite = limite;
        this.atuais = atuais;
    }

    public int getLimite() {
        return limite;
    }

    public int getAtuais() {
        return atuais;
    }
}
