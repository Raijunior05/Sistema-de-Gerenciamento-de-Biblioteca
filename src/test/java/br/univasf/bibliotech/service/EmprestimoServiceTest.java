package br.univasf.bibliotech.service;

import br.univasf.bibliotech.dao.EmprestimoDAO;
import br.univasf.bibliotech.dao.ItemDAO;
import br.univasf.bibliotech.dao.ReservaDAO;
import br.univasf.bibliotech.exception.ItemIndisponivelException;
import br.univasf.bibliotech.exception.LimiteExcedidoException;
import br.univasf.bibliotech.exception.PendenciaException;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Emprestimo;
import br.univasf.bibliotech.model.Item;
import br.univasf.bibliotech.model.Reserva;
import br.univasf.bibliotech.model.StatusEmprestimo;
import br.univasf.bibliotech.model.StatusReserva;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.util.Transacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Casos de Uso 10 e 12.
 *
 * <p>Cada teste corresponde a um passo ou fluxo alternativo da especificacao.
 * Os DAOs sao substituidos por dubles para que a regra seja testada em
 * isolamento, sem banco: se o teste falha, o defeito esta na regra.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmprestimoService - CU 10 e CU 12")
class EmprestimoServiceTest {

    private static final int LIMITE = 3;
    private static final int PRAZO_DIAS = 15;
    private static final int VALIDADE_RESERVA = 7;

    @Mock private EmprestimoDAO emprestimoDAO;
    @Mock private ItemDAO itemDAO;
    @Mock private ReservaDAO reservaDAO;

    private EmprestimoService service;
    private Usuario usuario;
    private Usuario administrador;
    private Item item;

    @BeforeEach
    void preparar() {
        service = new EmprestimoService(emprestimoDAO, itemDAO, reservaDAO,
                Transacao.direta(), LIMITE, PRAZO_DIAS, VALIDADE_RESERVA);

        usuario = new Usuario(1L, "Carlos Alberto Souza");
        usuario.setCpf("111.111.111-11");

        administrador = new Usuario(99L, "Administrador do sistema");

        item = new Item(10L, "Dom Casmurro", 2);
    }

    @Nested
    @DisplayName("CU 10 - Realizar Emprestimo")
    class RealizarEmprestimo {

        @Test
        @DisplayName("fluxo principal: registra o emprestimo e baixa o acervo")
        void deveRegistrarEmprestimoNoFluxoPrincipal() {
            when(itemDAO.quantidadeDisponivel(10L)).thenReturn(2);
            when(emprestimoDAO.contarAtivosPorUsuario(1L)).thenReturn(0);
            when(emprestimoDAO.possuiPendencia(1L)).thenReturn(false);
            when(itemDAO.decrementarDisponivel(10L)).thenReturn(true);

            Emprestimo resultado = service.registrar(usuario, item, administrador);

            // passo 08: data prevista calculada pelo sistema, nao digitada
            assertEquals(LocalDate.now().plusDays(PRAZO_DIAS), resultado.getDataPrevista());
            assertEquals(StatusEmprestimo.EM_ANDAMENTO, resultado.getStatus());

            verify(emprestimoDAO).inserir(any(Emprestimo.class));
            verify(itemDAO).decrementarDisponivel(10L);  // passo 09
        }

        @Test
        @DisplayName("fluxo 3.1: item sem exemplar disponivel encerra o emprestimo")
        void deveRecusarQuandoItemIndisponivel() {
            when(itemDAO.quantidadeDisponivel(10L)).thenReturn(0);

            ItemIndisponivelException erro = assertThrows(ItemIndisponivelException.class,
                    () -> service.registrar(usuario, item, administrador));

            assertEquals(item, erro.getItem());
            verify(emprestimoDAO, never()).inserir(any());
            verify(itemDAO, never()).decrementarDisponivel(anyLong());
        }

        @Test
        @DisplayName("fluxo 7.1: limite de emprestimos atingido")
        void deveRecusarQuandoLimiteAtingido() {
            when(itemDAO.quantidadeDisponivel(10L)).thenReturn(2);
            when(emprestimoDAO.contarAtivosPorUsuario(1L)).thenReturn(LIMITE);

            LimiteExcedidoException erro = assertThrows(LimiteExcedidoException.class,
                    () -> service.registrar(usuario, item, administrador));

            assertEquals(LIMITE, erro.getLimite());
            assertEquals(LIMITE, erro.getAtuais());
            verify(emprestimoDAO, never()).inserir(any());
        }

        @Test
        @DisplayName("fluxo 7.1: usuario com item em atraso")
        void deveRecusarQuandoHaPendencia() {
            when(itemDAO.quantidadeDisponivel(10L)).thenReturn(2);
            when(emprestimoDAO.contarAtivosPorUsuario(1L)).thenReturn(1);
            when(emprestimoDAO.possuiPendencia(1L)).thenReturn(true);

            assertThrows(PendenciaException.class,
                    () -> service.registrar(usuario, item, administrador));

            verify(emprestimoDAO, never()).inserir(any());
            verify(itemDAO, never()).decrementarDisponivel(anyLong());
        }

        @Test
        @DisplayName("fluxo 3.1: exemplar separado para o primeiro da fila nao e emprestado a outro")
        void deveRecusarExemplarSeparadoParaOutroUsuario() {
            when(itemDAO.quantidadeDisponivel(10L)).thenReturn(1);
            when(reservaDAO.listarPorItem(10L)).thenReturn(List.of(
                    reserva(new Usuario(2L, "Amanda"), StatusReserva.DISPONIVEL)));

            assertThrows(ItemIndisponivelException.class,
                    () -> service.registrar(usuario, item, administrador));

            verify(emprestimoDAO, never()).inserir(any());
            verify(itemDAO, never()).decrementarDisponivel(anyLong());
        }

        @Test
        @DisplayName("CU 12 fluxo 7.1: o primeiro da fila retira o exemplar e a reserva e atendida")
        void devePermitirRetiradaPeloPrimeiroDaFila() {
            Reserva reservaDoUsuario = reserva(usuario, StatusReserva.DISPONIVEL);
            when(itemDAO.quantidadeDisponivel(10L)).thenReturn(1);
            when(reservaDAO.listarPorItem(10L)).thenReturn(List.of(reservaDoUsuario));
            when(emprestimoDAO.contarAtivosPorUsuario(1L)).thenReturn(0);
            when(emprestimoDAO.possuiPendencia(1L)).thenReturn(false);
            when(itemDAO.decrementarDisponivel(10L)).thenReturn(true);

            service.registrar(usuario, item, administrador);

            verify(emprestimoDAO).inserir(any(Emprestimo.class));
            assertEquals(StatusReserva.ATENDIDA, reservaDoUsuario.getStatus());
            verify(reservaDAO).atualizar(reservaDoUsuario);
        }

        @Test
        @DisplayName("passo 03: exemplares separados para reserva nao contam como disponiveis")
        void deveDescontarExemplaresSeparados() {
            when(itemDAO.quantidadeDisponivel(10L)).thenReturn(2);
            when(reservaDAO.listarPorItem(10L)).thenReturn(List.of(
                    reserva(new Usuario(2L, "Amanda"), StatusReserva.DISPONIVEL),
                    reserva(new Usuario(3L, "Bruno"), StatusReserva.AGUARDANDO)));

            assertEquals(1, service.verificarDisponibilidade(10L));
        }

        @Test
        @DisplayName("exige identificacao previa do usuario (CU 9)")
        void deveExigirUsuarioIdentificado() {
            assertThrows(RegraNegocioException.class,
                    () -> service.registrar(null, item, administrador));
        }
    }

    @Nested
    @DisplayName("CU 12 - Realizar Devolucao")
    class RealizarDevolucao {

        @Test
        @DisplayName("fluxo principal: conclui o emprestimo e repoe o acervo")
        void deveConcluirDevolucaoNoPrazo() {
            Emprestimo emprestimo = emprestimoComPrazo(LocalDate.now().plusDays(3));
            when(reservaDAO.primeiroDaFila(10L)).thenReturn(Optional.empty());

            Optional<Reserva> fila = service.registrarDevolucao(emprestimo);

            assertEquals(StatusEmprestimo.CONCLUIDO, emprestimo.getStatus());
            assertEquals(LocalDate.now(), emprestimo.getDataDevolucao());
            assertEquals(0, emprestimo.getDiasAtraso());
            assertTrue(fila.isEmpty());

            verify(itemDAO).incrementarDisponivel(10L);  // passo 07
        }

        @Test
        @DisplayName("fluxo 5.1: devolucao com atraso registra dias e conclui")
        void deveRegistrarDiasQuandoHaAtraso() {
            Emprestimo emprestimo = emprestimoComPrazo(LocalDate.now().minusDays(4));
            when(reservaDAO.primeiroDaFila(10L)).thenReturn(Optional.empty());

            service.registrarDevolucao(emprestimo);

            assertEquals(4, emprestimo.getDiasAtraso());
            assertEquals(usuario, emprestimo.getUsuario());
            verify(emprestimoDAO).atualizar(emprestimo);
            verify(itemDAO).incrementarDisponivel(10L);
            assertEquals(StatusEmprestimo.CONCLUIDO, emprestimo.getStatus());
        }

        @Test
        @DisplayName("fluxo 7.1: separa o exemplar para o primeiro da fila")
        void deveSepararExemplarParaPrimeiroDaFila() {
            Emprestimo emprestimo = emprestimoComPrazo(LocalDate.now().plusDays(1));

            Reserva primeira = new Reserva(new Usuario(2L, "Amanda"), item, 1);
            primeira.setStatus(StatusReserva.AGUARDANDO);
            when(reservaDAO.primeiroDaFila(10L)).thenReturn(Optional.of(primeira));

            Optional<Reserva> fila = service.registrarDevolucao(emprestimo);

            assertTrue(fila.isPresent());
            assertTrue(fila.get().temPrioridade());
            assertEquals(StatusReserva.DISPONIVEL, primeira.getStatus());
            assertEquals(LocalDate.now().plusDays(VALIDADE_RESERVA), primeira.getValidadeMaxima());
            verify(reservaDAO).atualizar(primeira);
        }

        @Test
        @DisplayName("nao permite devolver um emprestimo ja concluido")
        void deveRecusarDevolucaoDuplicada() {
            Emprestimo emprestimo = emprestimoComPrazo(LocalDate.now());
            emprestimo.setStatus(StatusEmprestimo.CONCLUIDO);

            assertThrows(RegraNegocioException.class,
                    () -> service.registrarDevolucao(emprestimo));

            verify(emprestimoDAO, never()).atualizar(any());
        }

        private Emprestimo emprestimoComPrazo(LocalDate prevista) {
            Emprestimo e = new Emprestimo(usuario, item, prevista);
            e.setId(500L);
            e.setCodigo("EMP-1001");
            e.setStatus(StatusEmprestimo.EM_ANDAMENTO);
            return e;
        }
    }

    private Reserva reserva(Usuario dono, StatusReserva status) {
        Reserva r = new Reserva(dono, item, 1);
        r.setStatus(status);
        return r;
    }

    @Nested
    @DisplayName("CU 12, fluxo 5.1 - dias de atraso")
    class CalculoAtraso {
        @Test
        @DisplayName("passo 05: vencimento hoje nao tem atraso")
        void deveAceitarDevolucaoNoVencimento() {
            Emprestimo e = new Emprestimo(usuario, item, LocalDate.now());
            assertEquals(0, e.calcularDiasAtraso(LocalDate.now()));
        }

        @Test
        @DisplayName("fluxo 5.1: historico usa data real de devolucao")
        void devePreservarAtrasoHistorico() {
            Emprestimo e = new Emprestimo(usuario, item, LocalDate.of(2026, 9, 1));
            e.setDataDevolucao(LocalDate.of(2026, 9, 5));
            assertEquals(4, e.calcularDiasAtraso(LocalDate.of(2026, 10, 1)));
        }
    }
}
