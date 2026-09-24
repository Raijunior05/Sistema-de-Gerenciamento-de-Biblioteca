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

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Casos de Uso 10 (Realizar Emprestimo) e 12 (Realizar Devolucao).
 *
 * <p>Concentra as regras que o prototipo delegava ao Administrador:
 * o prazo de devolucao e calculado, nao digitado, e o calculo de atraso
 * acontece automaticamente no ato da devolucao. Registro e baixa no acervo
 * acontecem na mesma transacao.</p>
 */
public class EmprestimoService {

    private final EmprestimoDAO emprestimoDAO;
    private final ItemDAO itemDAO;
    private final ReservaDAO reservaDAO;
    private final Transacao transacao;

    private final int limiteEmprestimos;
    private final int diasPrazo;
    private final int diasValidadeReserva;

    /** CU 10 passos 07 e 08 e CU 12 fluxo 7.1: dependencias e parametros configurados. */
    public EmprestimoService(EmprestimoDAO emprestimoDAO, ItemDAO itemDAO, ReservaDAO reservaDAO,
                             Transacao transacao, int limiteEmprestimos, int diasPrazo,
                             int diasValidadeReserva) {
        this.emprestimoDAO = emprestimoDAO;
        this.itemDAO = itemDAO;
        this.reservaDAO = reservaDAO;
        this.transacao = transacao;
        this.limiteEmprestimos = limiteEmprestimos;
        this.diasPrazo = diasPrazo;
        this.diasValidadeReserva = diasValidadeReserva;
    }

    /**
     * CU 10 passo 03 - verificarDisponibilidade(id_item).
     *
     * <p>Exemplares separados para o primeiro da fila (CU 12, fluxo 7.1) nao
     * contam como livres.</p>
     *
     * @return exemplares que podem ser emprestados a qualquer usuario
     */
    public int verificarDisponibilidade(long itemId) {
        int disponiveis = itemDAO.quantidadeDisponivel(itemId);
        long separados = reservasAtivas(itemId).stream()
                .filter(r -> r.getStatus() == StatusReserva.DISPONIVEL)
                .count();
        return (int) Math.max(0, disponiveis - separados);
    }

    /**
     * CU 10 - Realizar Emprestimo, fluxo principal.
     *
     * <p>Ordem das validacoes, seguindo os passos da especificacao:
     * disponibilidade (passo 03), limite e pendencias (passo 07),
     * registro (passo 08) e baixa no acervo (passo 09). O exemplar separado
     * para o primeiro da fila so pode ser retirado por ele (CU 12, fluxo 7.1).</p>
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
            throw new RegraNegocioException("Identifique o usuário antes de registrar o empréstimo.");
        }
        if (item == null || item.getId() == null) {
            throw new RegraNegocioException("Selecione um item do acervo.");
        }

        return transacao.executar(() -> {
            // Passo 03 - disponibilidade
            int disponiveis = itemDAO.quantidadeDisponivel(item.getId());
            if (disponiveis <= 0) {
                throw new ItemIndisponivelException(item);
            }

            // CU 12 fluxo 7.1 - exemplar separado para o primeiro da fila
            List<Reserva> ativas = reservasAtivas(item.getId());
            Optional<Reserva> reservaDoUsuario = ativas.stream()
                    .filter(r -> Objects.equals(r.getUsuario().getId(), usuario.getId()))
                    .findFirst();
            long separadosParaOutros = ativas.stream()
                    .filter(r -> r.getStatus() == StatusReserva.DISPONIVEL)
                    .filter(r -> !Objects.equals(r.getUsuario().getId(), usuario.getId()))
                    .count();
            if (disponiveis <= separadosParaOutros) {
                throw new ItemIndisponivelException(item, "O exemplar disponível de \""
                        + item.getTitulo() + "\" está separado para o primeiro Usuário da fila "
                        + "de reserva. Deseja registrar uma reserva?");
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

            // Passo 09 - baixa na disponibilidade (falha desfaz o passo 08)
            if (!itemDAO.decrementarDisponivel(item.getId())) {
                throw new ItemIndisponivelException(item);
            }

            // A reserva do usuario para este item foi atendida pelo emprestimo
            reservaDoUsuario.ifPresent(reserva -> {
                reserva.setStatus(StatusReserva.ATENDIDA);
                reservaDAO.atualizar(reserva);
            });

            return emprestimo;
        });
    }

    /**
     * CU 12 - Realizar Devolucao, passos 05 a 07.
     *
     * <p>Fluxo alternativo 5.1: registra os dias de atraso e conclui a devolucao.
     * Fluxo alternativo 7.1: o primeiro da fila passa a ter o exemplar separado
     * para retirada ate o fim da validade da reserva.</p>
     *
     * @param emprestimo emprestimo selecionado pelo Administrador
     * @return a reserva com prioridade de retirada, quando existir (fluxo 7.1)
     */
    public Optional<Reserva> registrarDevolucao(Emprestimo emprestimo) {
        if (emprestimo == null || emprestimo.getId() == null) {
            throw new RegraNegocioException("Selecione o empréstimo a ser devolvido.");
        }
        if (emprestimo.getStatus() == StatusEmprestimo.CONCLUIDO) {
            throw new RegraNegocioException("Este empréstimo já foi devolvido.");
        }

        return transacao.executar(() -> {
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

            // Fluxo 7.1 - primeiro da fila tem prioridade na retirada
            Optional<Reserva> primeiro = reservaDAO.primeiroDaFila(emprestimo.getItem().getId());
            primeiro.ifPresent(reserva -> {
                reserva.setStatus(StatusReserva.DISPONIVEL);
                reserva.setValidadeMaxima(hoje.plusDays(diasValidadeReserva));
                reservaDAO.atualizar(reserva);
            });
            return primeiro;
        });
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

    private List<Reserva> reservasAtivas(long itemId) {
        return reservaDAO.listarPorItem(itemId).stream()
                .filter(r -> r.getStatus().estaAtiva())
                .toList();
    }
}
