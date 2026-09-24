package br.univasf.bibliotech.service;

import br.univasf.bibliotech.dao.ItemDAO;
import br.univasf.bibliotech.dao.ReservaDAO;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Item;
import br.univasf.bibliotech.model.Reserva;
import br.univasf.bibliotech.model.StatusReserva;
import br.univasf.bibliotech.model.Usuario;

import java.time.LocalDate;
import java.util.List;

/** Caso de Uso 11 - Realizar Reserva. */
public class ReservaService {

    private final ReservaDAO reservaDAO;
    private final ItemDAO itemDAO;
    private final int diasValidade;

    public ReservaService(ReservaDAO reservaDAO, ItemDAO itemDAO, int diasValidade) {
        this.reservaDAO = reservaDAO;
        this.itemDAO = itemDAO;
        this.diasValidade = diasValidade;
    }

    /**
     * CU 11 - fluxo principal.
     *
     * @throws RegraNegocioException fluxo 3.1 (item disponivel)
     *                               ou fluxo 5.1 (reserva duplicada)
     */
    public Reserva registrar(Usuario usuario, Item item, Usuario administrador) {
        if (usuario == null || usuario.getId() == null) {
            throw new RegraNegocioException("Identifique o usuario antes de registrar a reserva.");
        }
        if (item == null || item.getId() == null) {
            throw new RegraNegocioException("Selecione um item do acervo.");
        }

        // Passo 03 e fluxo alternativo 3.1; exemplar separado para o 1o da fila nao esta livre
        long separados = reservaDAO.listarPorItem(item.getId()).stream()
                .filter(r -> r.getStatus() == StatusReserva.DISPONIVEL)
                .count();
        if (itemDAO.quantidadeDisponivel(item.getId()) > separados) {
            throw new RegraNegocioException(
                    "O item possui exemplar disponivel. Realize o emprestimo em vez da reserva.");
        }

        // Fluxo alternativo 5.1
        if (reservaDAO.possuiReservaAtiva(usuario.getId(), item.getId())) {
            throw new RegraNegocioException(
                    "Este usuario ja possui uma reserva ativa para o mesmo item.");
        }

        // Passo 05 - entra no fim da fila
        LocalDate hoje = LocalDate.now();
        Reserva reserva = new Reserva(usuario, item, reservaDAO.proximaPosicaoFila(item.getId()));
        reserva.setDataReserva(hoje);
        reserva.setValidadeMaxima(hoje.plusDays(diasValidade));
        reserva.setStatus(StatusReserva.AGUARDANDO);
        reserva.setRegistradoPor(administrador);

        reservaDAO.inserir(reserva);
        return reserva;
    }

    public void cancelar(Reserva reserva) {
        if (reserva == null || reserva.getId() == null) {
            throw new RegraNegocioException("Selecione a reserva a ser cancelada.");
        }
        reserva.setStatus(StatusReserva.CANCELADA);
        reservaDAO.atualizar(reserva);
    }

    public List<Reserva> pesquisar(String termo, StatusReserva status, boolean crescente) {
        return reservaDAO.pesquisar(termo, status, crescente);
    }

    /** Executado no start: marca como expiradas as reservas vencidas. */
    public int expirarVencidas() {
        return reservaDAO.expirarVencidas(LocalDate.now());
    }
}
