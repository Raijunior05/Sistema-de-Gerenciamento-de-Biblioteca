package br.univasf.bibliotech.dao;

import br.univasf.bibliotech.model.Emprestimo;
import br.univasf.bibliotech.model.StatusEmprestimo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Acesso a dados de emprestimos (Casos de Uso 10 e 12). */
public interface EmprestimoDAO {

    /** CU 10 passo 08. Preenche id e codigo gerados. */
    void inserir(Emprestimo emprestimo);

    /** CU 12 passo 06. */
    void atualizar(Emprestimo emprestimo);

    Optional<Emprestimo> buscarPorId(long id);

    Optional<Emprestimo> buscarPorCodigo(String codigo);

    /** CU 12 passo 04: emprestimos em aberto de um usuario. */
    List<Emprestimo> listarAbertosPorUsuario(long usuarioId);

    List<Emprestimo> listarPorUsuario(long usuarioId);

    /** CU 10 passo 07: quantidade de emprestimos em aberto. */
    int contarAtivosPorUsuario(long usuarioId);

    /** CU 10 passo 07: existe emprestimo aberto com prazo vencido na data atual. */
    boolean possuiPendencia(long usuarioId);

    /** Tela "Consultar Emprestimos": busca livre, status e periodo. */
    List<Emprestimo> pesquisar(String termo, StatusEmprestimo status,
                               LocalDate inicio, LocalDate fim);

    List<Emprestimo> listarPorPeriodo(LocalDate inicio, LocalDate fim);

    /** Cartoes da visao geral. */
    int contarPorStatus(StatusEmprestimo status);

    /** Marca como ATRASADO os emprestimos cujo prazo venceu. */
    int marcarAtrasados(LocalDate referencia);

    String gerarProximoCodigo();
}
