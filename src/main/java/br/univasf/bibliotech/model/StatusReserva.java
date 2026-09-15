package br.univasf.bibliotech.model;

/**
 * Estados de uma reserva (CU 11).
 *
 * <p>DISPONIVEL representa o momento descrito no fluxo alternativo 7.1 do
 * CU 12: o item foi devolvido e o primeiro da fila tem prioridade na retirada.</p>
 */
public enum StatusReserva {

    AGUARDANDO("Pendente"),
    DISPONIVEL("Disponivel"),
    ATENDIDA("Atendida"),
    EXPIRADA("Expirada"),
    CANCELADA("Cancelada");

    private final String rotulo;

    StatusReserva(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getClasseCss() {
        return switch (this) {
            case DISPONIVEL -> "badge-disponivel";
            case AGUARDANDO -> "badge-pendente";
            case EXPIRADA   -> "badge-atrasado";
            default         -> "badge-neutro";
        };
    }

    public boolean estaAtiva() {
        return this == AGUARDANDO || this == DISPONIVEL;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
