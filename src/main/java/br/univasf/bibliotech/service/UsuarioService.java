package br.univasf.bibliotech.service;

import br.univasf.bibliotech.dao.UsuarioDAO;
import br.univasf.bibliotech.exception.DadosDuplicadosException;
import br.univasf.bibliotech.exception.ExclusaoNaoPermitidaException;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.exception.UsuarioNaoEncontradoException;
import br.univasf.bibliotech.model.Perfil;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.util.Senhas;

import java.util.List;

/**
 * Casos de Uso 3 a 6 (Cadastrar, Editar, Excluir e Consultar Usuario)
 * e Caso de Uso 9 (Identificar Usuario).
 */
public class UsuarioService {

    private final UsuarioDAO usuarioDAO;

    public UsuarioService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    /**
     * CU 3 - Cadastrar Usuario.
     *
     * @param usuario dados vindos do formulario
     * @param senha   obrigatoria apenas para o perfil Administrador
     * @throws RegraNegocioException      campo obrigatorio ausente
     * @throws DadosDuplicadosException   fluxo alternativo 4.1
     */
    public void cadastrar(Usuario usuario, char[] senha) {
        validarCamposObrigatorios(usuario);
        validarDuplicidade(usuario, null);

        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {
            if (senha == null || senha.length == 0) {
                throw new RegraNegocioException(
                        "O perfil Administrador exige a definicao de uma senha.");
            }
            usuario.setSenhaHash(Senhas.gerarHash(senha));
        } else {
            // Usuario comum nao acessa o sistema: sem login e sem senha.
            usuario.setLogin(null);
            usuario.setSenhaHash(null);
        }

        usuarioDAO.inserir(usuario);
    }

    /**
     * CU 4 - Editar Usuario.
     *
     * @param novaSenha quando nao nula, substitui a senha atual
     */
    public void editar(Usuario usuario, char[] novaSenha) {
        if (usuario.getId() == null) {
            throw new RegraNegocioException("Usuario sem identificador nao pode ser editado.");
        }
        validarCamposObrigatorios(usuario);
        validarDuplicidade(usuario, usuario.getId());

        if (novaSenha != null && novaSenha.length > 0) {
            usuario.setSenhaHash(Senhas.gerarHash(novaSenha));
        }
        usuarioDAO.atualizar(usuario);
    }

    /**
     * CU 5 - Excluir Usuario, fluxo alternativo 4.1.
     *
     * @param id                 usuario a remover
     * @param idAdministradorLogado para impedir auto-exclusao
     */
    public void excluir(long id, long idAdministradorLogado) {
        Usuario alvo = usuarioDAO.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Usuario nao encontrado."));

        if (id == idAdministradorLogado) {
            throw new ExclusaoNaoPermitidaException(
                    "nao e possivel excluir o proprio administrador logado.");
        }
        if (usuarioDAO.possuiEmprestimoOuReservaAtiva(id)) {
            throw new ExclusaoNaoPermitidaException(
                    "o usuario possui emprestimo ou reserva ativa.");
        }
        if (alvo.getPerfil() == Perfil.ADMINISTRADOR && usuarioDAO.contarAdministradores() <= 1) {
            throw new ExclusaoNaoPermitidaException(
                    "este e o unico administrador cadastrado.");
        }
        usuarioDAO.excluir(id);
    }

    /** CU 6 - Consultar Usuario. */
    public List<Usuario> pesquisar(String termo) {
        return usuarioDAO.pesquisar(termo);
    }

    public List<Usuario> listarTodos() {
        return usuarioDAO.listarTodos();
    }

    /**
     * CU 9 - Identificar Usuario.
     *
     * @param cpfOuMatricula identificacao informada pelo Usuario
     * @throws UsuarioNaoEncontradoException fluxo alternativo 4.1
     */
    public Usuario identificar(String cpfOuMatricula) {
        if (cpfOuMatricula == null || cpfOuMatricula.isBlank()) {
            throw new UsuarioNaoEncontradoException("(vazia)");
        }
        return usuarioDAO.buscarPorIdentificacao(cpfOuMatricula.trim())
                .orElseThrow(() -> new UsuarioNaoEncontradoException(cpfOuMatricula));
    }

    private void validarCamposObrigatorios(Usuario u) {
        if (u.getNome() == null || u.getNome().isBlank()) {
            throw new RegraNegocioException("Informe o nome do usuario.");
        }
        if (u.getEmail() == null || u.getEmail().isBlank()) {
            throw new RegraNegocioException("Informe o e-mail do usuario.");
        }
        if (!u.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new RegraNegocioException("E-mail em formato invalido.");
        }
        boolean semCpf = u.getCpf() == null || u.getCpf().isBlank();
        boolean semMatricula = u.getMatricula() == null || u.getMatricula().isBlank();
        if (semCpf && semMatricula) {
            throw new RegraNegocioException("Informe o CPF ou a matricula do usuario.");
        }
    }

    private void validarDuplicidade(Usuario u, Long ignorarId) {
        if (usuarioDAO.existeEmail(u.getEmail(), ignorarId)) {
            throw new DadosDuplicadosException("e-mail", u.getEmail());
        }
        if (usuarioDAO.existeCpf(u.getCpf(), ignorarId)) {
            throw new DadosDuplicadosException("CPF", u.getCpf());
        }
        if (usuarioDAO.existeMatricula(u.getMatricula(), ignorarId)) {
            throw new DadosDuplicadosException("matricula", u.getMatricula());
        }
        if (u.getLogin() != null && usuarioDAO.existeLogin(u.getLogin(), ignorarId)) {
            throw new DadosDuplicadosException("nome de usuario", u.getLogin());
        }
    }
}
