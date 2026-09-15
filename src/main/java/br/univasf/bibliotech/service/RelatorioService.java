package br.univasf.bibliotech.service;

import br.univasf.bibliotech.dao.EmprestimoDAO;
import br.univasf.bibliotech.dao.ReservaDAO;
import br.univasf.bibliotech.dao.UsuarioDAO;
import br.univasf.bibliotech.model.Emprestimo;
import br.univasf.bibliotech.model.StatusEmprestimo;
import br.univasf.bibliotech.model.StatusReserva;

import java.time.LocalDate;
import java.util.List;

/**
 * Caso de Uso 13 - Gerar Relatorio.
 *
 * <p>Alimenta tambem os cartoes da tela "Visao Geral" e o painel
 * "Resumo dos dados (previa)" da tela de relatorios.</p>
 */
public class RelatorioService {

    /** Tipos disponiveis no combo "Tipo de relatorio" do prototipo. */
    public enum Tipo {
        EMPRESTIMOS_E_ATRASOS("Relatorio de emprestimos e atrasos"),
        ITENS_RESERVADOS("Relatorio de itens reservados e disponiveis"),
        HISTORICO_POR_USUARIO("Historico de emprestimos por usuario");

        private final String rotulo;

        Tipo(String rotulo) {
            this.rotulo = rotulo;
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

    /** Numeros exibidos nos cartoes da visao geral e da previa. */
    public record Resumo(int emprestimosAtivos,
                         int livrosAtrasados,
                         int reservasPendentes,
                         int devolucoesConcluidas,
                         int novosUsuarios) {
    }

    private final EmprestimoDAO emprestimoDAO;
    private final ReservaDAO reservaDAO;
    private final UsuarioDAO usuarioDAO;

    public RelatorioService(EmprestimoDAO emprestimoDAO, ReservaDAO reservaDAO,
                            UsuarioDAO usuarioDAO) {
        this.emprestimoDAO = emprestimoDAO;
        this.reservaDAO = reservaDAO;
        this.usuarioDAO = usuarioDAO;
    }

    public Resumo resumoGeral() {
        return new Resumo(
                emprestimoDAO.contarPorStatus(StatusEmprestimo.EM_ANDAMENTO),
                emprestimoDAO.contarPorStatus(StatusEmprestimo.ATRASADO),
                reservaDAO.contarPorStatus(StatusReserva.AGUARDANDO),
                emprestimoDAO.contarPorStatus(StatusEmprestimo.CONCLUIDO),
                usuarioDAO.contarCadastradosDesde(LocalDate.now().withDayOfMonth(1)));
    }

    /** CU 13 passo 04: gera os dados do relatorio no periodo informado. */
    public List<Emprestimo> gerar(Tipo tipo, LocalDate inicio, LocalDate fim) {
        return switch (tipo) {
            case EMPRESTIMOS_E_ATRASOS, HISTORICO_POR_USUARIO ->
                    emprestimoDAO.listarPorPeriodo(inicio, fim);
            case ITENS_RESERVADOS ->
                    List.of();
        };
    }
}
