package br.univasf.bibliotech.model;

/**
 * Estados de um emprestimo.
 *
 * <p>Os rotulos correspondem aos badges coloridos do prototipo:
 * ATIVO em verde, ATRASADO em vermelho, DEVOLVIDO em cinza.</p>
 */
public enum StatusEmprestimo {

    EM_ANDAMENTO("Ativo"),
    ATRASADO("Atrasado"),
    CONCLUIDO("Devolvido");

    private final String rotulo;

    StatusEmprestimo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Classe CSS do badge, definida em sgb.css. */
    public String getClasseCss() {
        return switch (this) {
            case EM_ANDAMENTO -> "badge-ativo";
            case ATRASADO     -> "badge-atrasado";
            case CONCLUIDO    -> "badge-neutro";
        };
    }

    public boolean estaAberto() {
        return this != CONCLUIDO;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
