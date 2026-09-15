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
import br.univasf.bibliotech.model.Usuario;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Casos de Uso 10 (Realizar Emprestimo) e 12 (Realizar Devolucao).
 *
 * <p>Concentra as regras que o prototipo delegava ao Administrador:
 * o prazo de devolucao e calculado, nao digitado, e o calculo de atraso
 * acontece automaticamente no ato da devolucao.</p>
 */
public class EmprestimoService {

    private final EmprestimoDAO emprestimoDAO;
    private final ItemDAO itemDAO;
    private final ReservaDAO reservaDAO;

    private final int limiteEmprestimos;
    private final int diasPrazo;

    /** CU 10 passos 07 e 08 e CU 12: dependencias e parametros de limite e prazo. */
    public EmprestimoService(EmprestimoDAO emprestimoDAO, ItemDAO itemDAO, ReservaDAO reservaDAO,
                             int limiteEmprestimos, int diasPrazo) {
        this.emprestimoDAO = emprestimoDAO;
        this.itemDAO = itemDAO;
        this.reservaDAO = reservaDAO;
        this.limiteEmprestimos = limiteEmprestimos;
        this.diasPrazo = diasPrazo;
    }

    /**
     * CU 10 - Realizar Emprestimo, fluxo principal.
     *
     * <p>Ordem das validacoes, seguindo os passos da especificacao:
     * disponibilidade (passo 03), limite e pendencias (passo 07),
     * registro (passo 08) e baixa no acervo (passo 09).</p>
     *
     * @param usuario Usuario ja identificado pelo CU 9
     * @param item    item selecionado do acervo
     * @param administrador quem esta operando o sistema
     * @return o emprestimo registrado, com codigo e data prevista preenchidos
     * @throws ItemIndisponivelException fluxo alternativo 3.1
     * @throws LimiteExcedidoException   fluxo alternativo 7.1
     * @throws PendenciaException        fluxo alternativo 7.1
     */
    public Emprestimo registrar(Usuario usuario, Item item, Usuario administrador) {
        if (usuario == null || usuario.getId() == null) {
            throw new RegraNegocioException("Identifique o usuario antes de registrar o emprestimo.");
        }
        if (item == null || item.getId() == null) {
            throw new RegraNegocioException("Selecione um item do acervo.");
        }

        // Passo 03 - disponibilidade
        if (itemDAO.quantidadeDisponivel(item.getId()) <= 0) {
            throw new ItemIndisponivelException(item);
        }

        // Passo 07 - limite de emprestimos
        int ativos = emprestimoDAO.contarAtivosPorUsuario(usuario.getId());
        if (ativos >= limiteEmprestimos) {
            throw new LimiteExcedidoException(limiteEmprestimos, ativos);
        }

        // Passo 07 - pendencias (itens em atraso)
        if (emprestimoDAO.possuiPendencia(usuario.getId())) {
            throw new PendenciaException("há item em atraso.");
        }

        // Passo 08 - registro com prazo calculado pelo sistema
        LocalDate hoje = LocalDate.now();
        Emprestimo emprestimo = new Emprestimo(usuario, item, hoje.plusDays(diasPrazo));
        emprestimo.setDataEmprestimo(hoje);
        emprestimo.setStatus(StatusEmprestimo.EM_ANDAMENTO);
        emprestimo.setRegistradoPor(administrador);
        emprestimoDAO.inserir(emprestimo);

        // Passo 09 - baixa na disponibilidade
        if (!itemDAO.decrementarDisponivel(item.getId())) {
            throw new ItemIndisponivelException(item);
        }

        return emprestimo;
    }

    /**
     * CU 12 - Realizar Devolucao.
     *
     * <p>Fluxo alternativo 5.1: registra os dias de atraso e conclui a devolucao.</p>
     *
     * @param emprestimo emprestimo selecionado pelo Administrador
     * @return a reserva com prioridade de retirada, quando existir (fluxo 7.1)
     */
    public Optional<Reserva> registrarDevolucao(Emprestimo emprestimo) {
        if (emprestimo == null || emprestimo.getId() == null) {
            throw new RegraNegocioException("Selecione o emprestimo a ser devolvido.");
        }
        if (emprestimo.getStatus() == StatusEmprestimo.CONCLUIDO) {
            throw new RegraNegocioException("Este emprestimo ja foi devolvido.");
        }

        LocalDate hoje = LocalDate.now();

        // Passo 05 e fluxo 5.1 - comparacao de datas e calculo do atraso
        long atraso = emprestimo.calcularDiasAtraso(hoje);
        emprestimo.setDiasAtraso((int) atraso);

        // Passo 06 - conclusao do emprestimo
        emprestimo.setDataDevolucao(hoje);
        emprestimo.setStatus(StatusEmprestimo.CONCLUIDO);
        emprestimoDAO.atualizar(emprestimo);

        // Passo 07 - reposicao no acervo
        itemDAO.incrementarDisponivel(emprestimo.getItem().getId());

        // Fluxo 7.1 - primeiro da fila tem prioridade
        return reservaDAO.primeiroDaFila(emprestimo.getItem().getId());
    }

    /** CU 12 passo 04: emprestimos em aberto do usuario identificado. */
    public List<Emprestimo> listarAbertos(long usuarioId) {
        return emprestimoDAO.listarAbertosPorUsuario(usuarioId);
    }

    /** CU 13, fluxo principal: consulta de emprestimos com filtros na area de relatorios. */
    public List<Emprestimo> pesquisar(String termo, StatusEmprestimo status,
                                      LocalDate inicio, LocalDate fim) {
        return emprestimoDAO.pesquisar(termo, status, inicio, fim);
    }

    /**
     * CU 13, fluxo principal: atualiza o status dos emprestimos vencidos para consulta.
     * Executado no start da aplicacao para que a visao geral fique correta.
     *
     * @return quantidade de emprestimos marcados como atrasados
     */
    public int atualizarAtrasos() {
        return emprestimoDAO.marcarAtrasados(LocalDate.now());
    }

    /** CU 10 passo 07: limite configurado de emprestimos por usuario. */
    public int getLimiteEmprestimos() {
        return limiteEmprestimos;
    }

    /** CU 10 passo 08: prazo configurado para a devolucao. */
    public int getDiasPrazo() {
        return diasPrazo;
    }
}
