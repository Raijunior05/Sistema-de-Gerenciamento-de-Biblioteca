package br.univasf.bibliotech.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Reserva de um item indisponivel (Caso de Uso 11).
 *
 * <p>Os campos seguem a tela "Consultar Reservas Pendentes" do prototipo:
 * posicao na fila, leitor, obra solicitada, data da reserva, validade maxima
 * e status. O prototipo nao possui tela para <i>criar</i> a reserva, apenas
 * para consultar, e por isso o CU 11 exige uma tela nova.</p>
 */
public class Reserva {

    private Long id;
    private Usuario usuario;
    private Item item;
    private LocalDate dataReserva = LocalDate.now();
    private LocalDate validadeMaxima;
    private int posicaoFila;
    private StatusReserva status = StatusReserva.AGUARDANDO;
    private Usuario registradoPor;

    public Reserva() {
    }

    public Reserva(Usuario usuario, Item item, int posicaoFila) {
        this.usuario = usuario;
        this.item = item;
        this.posicaoFila = posicaoFila;
    }

    /** Rotulo da coluna "Posicao" do prototipo: "1o da Fila", "Expirada". */
    public String getRotuloPosicao() {
        if (status == StatusReserva.EXPIRADA || status == StatusReserva.CANCELADA) {
            return status.getRotulo();
        }
        return posicaoFila + "o da fila";
    }

    /** Verdadeiro quando a reserva perdeu a validade sem ser atendida. */
    public boolean expirou(LocalDate referencia) {
        return status.estaAtiva()
                && validadeMaxima != null
                && referencia.isAfter(validadeMaxima);
    }

    /** CU 12 fluxo 7.1: o primeiro da fila tem prioridade na retirada. */
    public boolean temPrioridade() {
        return posicaoFila == 1 && status.estaAtiva();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public LocalDate getDataReserva() {
        return dataReserva;
    }

    public void setDataReserva(LocalDate dataReserva) {
        this.dataReserva = dataReserva;
    }

    public LocalDate getValidadeMaxima() {
        return validadeMaxima;
    }

    public void setValidadeMaxima(LocalDate validadeMaxima) {
        this.validadeMaxima = validadeMaxima;
    }

    public int getPosicaoFila() {
        return posicaoFila;
    }

    public void setPosicaoFila(int posicaoFila) {
        this.posicaoFila = posicaoFila;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public void setStatus(StatusReserva status) {
        this.status = status;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Usuario registradoPor) {
        this.registradoPor = registradoPor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reserva outra)) {
            return false;
        }
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return getRotuloPosicao();
    }
}
