package br.univasf.bibliotech.dao;

import br.univasf.bibliotech.model.Usuario;

import java.util.List;
import java.util.Optional;

/** Acesso a dados de usuarios (Casos de Uso 3 a 6 e 9). */
public interface UsuarioDAO {

    /** CU 3 passo 05. Preenche o id gerado no objeto recebido. */
    void inserir(Usuario usuario);

    /** CU 4 passo 05. */
    void atualizar(Usuario usuario);

    /** CU 5 passo 04. */
    void excluir(long id);

    Optional<Usuario> buscarPorId(long id);

    /** CU 9 passo 04: busca por CPF ou matricula. */
    Optional<Usuario> buscarPorIdentificacao(String cpfOuMatricula);

    /** CU 1 passo 03: aceita e-mail (prototipo) ou login (especificacao). */
    Optional<Usuario> buscarPorCredencial(String emailOuLogin);

    /** CU 6 passo 02: busca por nome, e-mail, CPF ou matricula. */
    List<Usuario> pesquisar(String termo);

    List<Usuario> listarTodos();

    boolean existeEmail(String email, Long ignorarId);

    boolean existeCpf(String cpf, Long ignorarId);

    boolean existeMatricula(String matricula, Long ignorarId);

    boolean existeLogin(String login, Long ignorarId);

    /** CU 5 fluxo 4.1: bloqueia exclusao de quem tem pendencia. */
    boolean possuiEmprestimoOuReservaAtiva(long id);

    /** CU 5 fluxo 4.1: impede remover o unico administrador. */
    long contarAdministradores();

    /** Visao geral: total de usuarios cadastrados no periodo. */
    int contarCadastradosDesde(java.time.LocalDate inicio);
}
