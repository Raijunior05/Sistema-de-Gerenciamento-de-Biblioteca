package br.univasf.bibliotech.service;

import br.univasf.bibliotech.dao.ItemDAO;
import br.univasf.bibliotech.dao.ReservaDAO;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Item;
import br.univasf.bibliotech.model.Reserva;
import br.univasf.bibliotech.model.StatusReserva;
import br.univasf.bibliotech.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Caso de Uso 11 - Realizar Reserva. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaService - CU 11")
class ReservaServiceTest {

    private static final int VALIDADE_DIAS = 7;

    @Mock private ReservaDAO reservaDAO;
    @Mock private ItemDAO itemDAO;

    private ReservaService service;
    private Usuario usuario;
    private Usuario administrador;
    private Item item;

    @BeforeEach
    void preparar() {
        service = new ReservaService(reservaDAO, itemDAO, VALIDADE_DIAS);
        usuario = new Usuario(1L, "Amanda Siqueira Mendes");
        administrador = new Usuario(99L, "Administrador");
        item = new Item(10L, "O Hobbit", 0);
    }

    @Test
    @DisplayName("fluxo principal: entra no fim da fila com status aguardando")
    void deveRegistrarReservaNaFila() {
        when(itemDAO.quantidadeDisponivel(10L)).thenReturn(0);
        when(reservaDAO.possuiReservaAtiva(1L, 10L)).thenReturn(false);
        when(reservaDAO.proximaPosicaoFila(10L)).thenReturn(3);

        Reserva reserva = service.registrar(usuario, item, administrador);

        assertEquals(3, reserva.getPosicaoFila());
        assertEquals(StatusReserva.AGUARDANDO, reserva.getStatus());
        assertEquals(LocalDate.now().plusDays(VALIDADE_DIAS), reserva.getValidadeMaxima());
        verify(reservaDAO).inserir(reserva);
    }

    @Test
    @DisplayName("fluxo 3.1: item disponivel deve virar emprestimo, nao reserva")
    void deveRecusarQuandoItemDisponivel() {
        when(itemDAO.quantidadeDisponivel(10L)).thenReturn(2);

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> service.registrar(usuario, item, administrador));

        assertTrue(erro.getMessage().toLowerCase().contains("emprestimo"));
        verify(reservaDAO, never()).inserir(any());
    }

    @Test
    @DisplayName("fluxo 5.1: reserva duplicada para o mesmo item")
    void deveRecusarReservaDuplicada() {
        when(itemDAO.quantidadeDisponivel(10L)).thenReturn(0);
        when(reservaDAO.possuiReservaAtiva(1L, 10L)).thenReturn(true);

        assertThrows(RegraNegocioException.class,
                () -> service.registrar(usuario, item, administrador));

        verify(reservaDAO, never()).inserir(any());
    }

    @Test
    @DisplayName("fluxo 3.1: exemplar separado para o primeiro da fila nao impede nova reserva")
    void devePermitirReservaQuandoExemplarEstaSeparado() {
        Reserva separada = new Reserva(new Usuario(2L, "Bruno"), item, 1);
        separada.setStatus(StatusReserva.DISPONIVEL);
        when(itemDAO.quantidadeDisponivel(10L)).thenReturn(1);
        when(reservaDAO.listarPorItem(10L)).thenReturn(List.of(separada));
        when(reservaDAO.possuiReservaAtiva(1L, 10L)).thenReturn(false);
        when(reservaDAO.proximaPosicaoFila(10L)).thenReturn(2);

        Reserva reserva = service.registrar(usuario, item, administrador);

        assertEquals(2, reserva.getPosicaoFila());
        verify(reservaDAO).inserir(reserva);
    }
}
