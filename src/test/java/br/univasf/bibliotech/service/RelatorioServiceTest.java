package br.univasf.bibliotech.service;

import br.univasf.bibliotech.dao.EmprestimoDAO;
import br.univasf.bibliotech.dao.ItemDAO;
import br.univasf.bibliotech.dao.ReservaDAO;
import br.univasf.bibliotech.dao.UsuarioDAO;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Emprestimo;
import br.univasf.bibliotech.model.Item;
import br.univasf.bibliotech.model.Reserva;
import br.univasf.bibliotech.model.StatusEmprestimo;
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
import static org.mockito.Mockito.when;

/** Caso de Uso 13 - Gerar Relatorio: um teste por tipo do passo 01. */
@ExtendWith(MockitoExtension.class)
@DisplayName("RelatorioService - CU 13")
class RelatorioServiceTest {

    @Mock private EmprestimoDAO emprestimoDAO;
    @Mock private ReservaDAO reservaDAO;
    @Mock private UsuarioDAO usuarioDAO;
    @Mock private ItemDAO itemDAO;

    private RelatorioService service;
    private Usuario usuario;
    private Item item;

    @BeforeEach
    void preparar() {
        service = new RelatorioService(emprestimoDAO, reservaDAO, usuarioDAO, itemDAO);
        usuario = new Usuario(1L, "Carlos Alberto Souza");
        item = new Item(10L, "Dom Casmurro", 1);
        item.setQuantidadeTotal(2);
    }

    private Emprestimo emprestimo(StatusEmprestimo status, LocalDate retirada, LocalDate prevista) {
        Emprestimo e = new Emprestimo(usuario, item, prevista);
        e.setDataEmprestimo(retirada);
        e.setStatus(status);
        if (status == StatusEmprestimo.CONCLUIDO) {
            e.setDataDevolucao(prevista);
        }
        return e;
    }

    @Test
    @DisplayName("passo 04: itens emprestados exclui emprestimos concluidos")
    void deveListarSomenteEmprestimosAbertos() {
        LocalDate hoje = LocalDate.now();
        Emprestimo aberto = emprestimo(StatusEmprestimo.EM_ANDAMENTO, hoje, hoje.plusDays(5));
        Emprestimo concluido = emprestimo(StatusEmprestimo.CONCLUIDO, hoje, hoje.plusDays(5));
        when(emprestimoDAO.pesquisar(null, null, null, null)).thenReturn(List.of(aberto, concluido));

        assertEquals(List.of(aberto), service.itensEmprestados(null, null));
    }

    @Test
    @DisplayName("passo 04: atrasos inclui vencidos em aberto e devolucoes com atraso")
    void deveListarAtrasos() {
        LocalDate hoje = LocalDate.now();
        Emprestimo vencido = emprestimo(StatusEmprestimo.ATRASADO, hoje.minusDays(20), hoje.minusDays(5));
        Emprestimo noPrazo = emprestimo(StatusEmprestimo.EM_ANDAMENTO, hoje, hoje.plusDays(5));
        Emprestimo devolvidoComAtraso =
                emprestimo(StatusEmprestimo.CONCLUIDO, hoje.minusDays(30), hoje.minusDays(15));
        devolvidoComAtraso.setDataDevolucao(hoje.minusDays(12));
        when(emprestimoDAO.pesquisar(null, null, null, null))
                .thenReturn(List.of(vencido, noPrazo, devolvidoComAtraso));

        assertEquals(List.of(vencido, devolvidoComAtraso), service.atrasos(null, null));
    }

    @Test
    @DisplayName("passo 04: historico por usuario respeita o periodo")
    void deveFiltrarHistoricoPorPeriodo() {
        LocalDate inicio = LocalDate.of(2026, 9, 1);
        LocalDate fim = LocalDate.of(2026, 9, 30);
        Emprestimo dentro = emprestimo(StatusEmprestimo.CONCLUIDO,
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 25));
        Emprestimo fora = emprestimo(StatusEmprestimo.CONCLUIDO,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 25));
        when(emprestimoDAO.listarPorUsuario(1L)).thenReturn(List.of(dentro, fora));

        assertEquals(List.of(dentro), service.historicoPorUsuario(usuario, inicio, fim));
    }

    @Test
    @DisplayName("passo 04: itens reservados e disponiveis soma as reservas ativas por item")
    void deveContarReservasPorItem() {
        Item esgotado = new Item(20L, "O Hobbit", 0);
        Item semMovimento = new Item(30L, "Iracema", 0);
        Reserva aguardando = new Reserva(usuario, esgotado, 1);
        when(itemDAO.listarTodos()).thenReturn(List.of(item, esgotado, semMovimento));
        when(reservaDAO.pesquisar(null, StatusReserva.AGUARDANDO, true)).thenReturn(List.of(aguardando));
        when(reservaDAO.pesquisar(null, StatusReserva.DISPONIVEL, true)).thenReturn(List.of());

        List<RelatorioService.SituacaoItem> linhas = service.itensReservadosEDisponiveis("");

        assertEquals(2, linhas.size());
        assertEquals(item, linhas.get(0).item());
        assertEquals(1, linhas.get(1).reservasAguardando());
    }

    @Test
    @DisplayName("fluxo 4.1: sem registros para os filtros o resultado e vazio")
    void deveRetornarVazioSemRegistros() {
        when(emprestimoDAO.pesquisar(null, null, null, null)).thenReturn(List.of());

        assertTrue(service.itensEmprestados(null, null).isEmpty());
    }

    @Test
    @DisplayName("passo 03: periodo com inicio depois do fim e recusado")
    void deveRecusarPeriodoInvertido() {
        LocalDate hoje = LocalDate.now();

        assertThrows(RegraNegocioException.class,
                () -> service.atrasos(hoje, hoje.minusDays(1)));
    }
}
