package br.univasf.bibliotech.dao;

import br.univasf.bibliotech.model.Reserva;
import br.univasf.bibliotech.model.StatusReserva;

import java.util.List;
import java.util.Optional;

/** Acesso a dados de reservas (Caso de Uso 11). */
public interface ReservaDAO {

    /** CU 11 passo 05. */
    void inserir(Reserva reserva);

    void atualizar(Reserva reserva);

    Optional<Reserva> buscarPorId(long id);

    /** CU 11 fluxo 5.1: verifica reserva duplicada. */
    boolean possuiReservaAtiva(long usuarioId, long itemId);

    /** CU 11 passo 05: proxima posicao na fila de espera do item. */
    int proximaPosicaoFila(long itemId);

    /** CU 12 fluxo 7.1: primeiro da fila do item devolvido. */
    Optional<Reserva> primeiroDaFila(long itemId);

    /** Tela "Consultar Reservas Pendentes". */
    List<Reserva> pesquisar(String termo, StatusReserva status, boolean ordemCrescente);

    List<Reserva> listarPorItem(long itemId);

    List<Reserva> listarPorUsuario(long usuarioId);

    int contarPorStatus(StatusReserva status);

    /** Expira reservas vencidas e reordena a fila. */
    int expirarVencidas(java.time.LocalDate referencia);
}
