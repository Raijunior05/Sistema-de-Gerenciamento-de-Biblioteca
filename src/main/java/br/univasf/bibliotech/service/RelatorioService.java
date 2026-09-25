package br.univasf.bibliotech.service;

import br.univasf.bibliotech.dao.EmprestimoDAO;
import br.univasf.bibliotech.dao.ItemDAO;
import br.univasf.bibliotech.dao.ReservaDAO;
import br.univasf.bibliotech.dao.UsuarioDAO;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Emprestimo;
import br.univasf.bibliotech.model.Item;
import br.univasf.bibliotech.model.StatusEmprestimo;
import br.univasf.bibliotech.model.StatusReserva;
import br.univasf.bibliotech.model.Usuario;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Caso de Uso 13 - Gerar Relatorio.
 *
 * <p>Um metodo por tipo de relatorio do passo 01; os parametros de cada um
 * sao os filtros do passo 02. Alimenta tambem os cartoes da visao geral.</p>
 */
public class RelatorioService {

    /** CU 13 passo 01: tipos de relatorio da especificacao. */
    public enum Tipo {
        ITENS_EMPRESTADOS,
        ITENS_RESERVADOS_E_DISPONIVEIS,
        ATRASOS,
        HISTORICO_POR_USUARIO
    }

    /** Numeros exibidos nos cartoes da visao geral. */
    public record Resumo(int emprestimosAtivos,
                         int livrosAtrasados,
                         int reservasPendentes,
                         int devolucoesConcluidas,
                         int novosUsuarios) {
    }

    /** Linha do relatorio de itens reservados e disponiveis. */
    public record SituacaoItem(Item item, int reservasAguardando, int reservasParaRetirada) {
    }

    private final EmprestimoDAO emprestimoDAO;
    private final ReservaDAO reservaDAO;
    private final UsuarioDAO usuarioDAO;
    private final ItemDAO itemDAO;

    public RelatorioService(EmprestimoDAO emprestimoDAO, ReservaDAO reservaDAO,
                            UsuarioDAO usuarioDAO, ItemDAO itemDAO) {
        this.emprestimoDAO = emprestimoDAO;
        this.reservaDAO = reservaDAO;
        this.usuarioDAO = usuarioDAO;
        this.itemDAO = itemDAO;
    }

    /** Cartoes da visao geral, resumo dos dados usados pelo CU 13. */
    public Resumo resumoGeral() {
        return new Resumo(
                emprestimoDAO.contarPorStatus(StatusEmprestimo.EM_ANDAMENTO),
                emprestimoDAO.contarPorStatus(StatusEmprestimo.ATRASADO),
                reservaDAO.contarPorStatus(StatusReserva.AGUARDANDO),
                emprestimoDAO.contarPorStatus(StatusEmprestimo.CONCLUIDO),
                usuarioDAO.contarCadastradosDesde(LocalDate.now().withDayOfMonth(1)));
    }

    /**
     * CU 13 passo 04, tipo "itens emprestados": emprestimos em aberto
     * retirados no periodo informado.
     *
     * @throws RegraNegocioException periodo invalido
     */
    public List<Emprestimo> itensEmprestados(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        return emprestimoDAO.pesquisar(null, null, inicio, fim).stream()
                .filter(e -> e.getStatus() != StatusEmprestimo.CONCLUIDO)
                .toList();
    }

    /**
     * CU 13 passo 04, tipo "atrasos": emprestimos em aberto com prazo vencido
     * e devolucoes concluidas com dias de atraso (CU 12, fluxo 5.1).
     *
     * @throws RegraNegocioException periodo invalido
     */
    public List<Emprestimo> atrasos(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        LocalDate hoje = LocalDate.now();
        return emprestimoDAO.pesquisar(null, null, inicio, fim).stream()
                .filter(e -> e.calcularDiasAtraso(hoje) > 0)
                .toList();
    }

    /**
     * CU 13 passo 04, tipo "historico de emprestimos por Usuario".
     *
     * @param usuario Usuario identificado pelo CU 9
     * @throws RegraNegocioException usuario ausente ou periodo invalido
     */
    public List<Emprestimo> historicoPorUsuario(Usuario usuario, LocalDate inicio, LocalDate fim) {
        if (usuario == null || usuario.getId() == null) {
            throw new RegraNegocioException("Identifique o usuário do histórico.");
        }
        validarPeriodo(inicio, fim);
        return emprestimoDAO.listarPorUsuario(usuario.getId()).stream()
                .filter(e -> inicio == null || !e.getDataEmprestimo().isBefore(inicio))
                .filter(e -> fim == null || !e.getDataEmprestimo().isAfter(fim))
                .toList();
    }

    /**
     * CU 13 passo 04, tipo "itens reservados e disponiveis": itens com
     * exemplar disponivel ou com reserva ativa.
     *
     * @param termo titulo, autor, categoria, ISBN ou tombo; vazio lista o acervo
     */
    public List<SituacaoItem> itensReservadosEDisponiveis(String termo) {
        List<Item> itens = termo == null || termo.isBlank()
                ? itemDAO.listarTodos()
                : itemDAO.pesquisar(termo.trim());
        Map<Long, Long> aguardando = contarReservasPorItem(StatusReserva.AGUARDANDO);
        Map<Long, Long> paraRetirada = contarReservasPorItem(StatusReserva.DISPONIVEL);

        return itens.stream()
                .map(i -> new SituacaoItem(i,
                        aguardando.getOrDefault(i.getId(), 0L).intValue(),
                        paraRetirada.getOrDefault(i.getId(), 0L).intValue()))
                .filter(s -> s.item().getQuantidadeDisponivel() > 0
                        || s.reservasAguardando() + s.reservasParaRetirada() > 0)
                .toList();
    }

    private Map<Long, Long> contarReservasPorItem(StatusReserva status) {
        return reservaDAO.pesquisar(null, status, true).stream()
                .collect(Collectors.groupingBy(r -> r.getItem().getId(), Collectors.counting()));
    }

    private static void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new RegraNegocioException("A data inicial deve ser anterior ou igual à final.");
        }
    }
}
