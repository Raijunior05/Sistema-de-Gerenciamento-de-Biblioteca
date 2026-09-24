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
     * <p>Passo 03: o perfil Administrador exige tambem nome de usuario e senha.</p>
     *
     * @param usuario dados vindos do formulario
     * @param senha   obrigatoria apenas para o perfil Administrador
     * @throws RegraNegocioException      fluxo alternativo 4.1, campo obrigatorio ausente
     * @throws DadosDuplicadosException   fluxo alternativo 4.1, dado ja cadastrado
     */
    public void cadastrar(Usuario usuario, char[] senha) {
        validarCamposObrigatorios(usuario);
        validarDuplicidade(usuario, null);

        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {
            if (senha == null || senha.length == 0) {
                throw new RegraNegocioException(
                        "O perfil Administrador exige a definição de uma senha.");
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
     * CU 4 - Editar Usuario, passos 04 e 05.
     *
     * <p>Retirar o perfil Administrador segue as mesmas protecoes do CU 5,
     * fluxo 4.1: o sistema nao pode ficar sem Administrador nem o operador
     * pode retirar o proprio acesso.</p>
     *
     * @param novaSenha             quando nao nula, substitui a senha atual
     * @param idAdministradorLogado Administrador que opera o sistema
     * @throws RegraNegocioException    fluxo alternativo 4.1, dados invalidos
     * @throws DadosDuplicadosException fluxo alternativo 4.1, dado ja cadastrado
     */
    public void editar(Usuario usuario, char[] novaSenha, long idAdministradorLogado) {
        if (usuario.getId() == null) {
            throw new RegraNegocioException("Usuário sem identificador não pode ser editado.");
        }
        validarCamposObrigatorios(usuario);
        validarDuplicidade(usuario, usuario.getId());

        Usuario atual = usuarioDAO.buscarPorId(usuario.getId())
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
        boolean perdeAdministrador = atual.getPerfil() == Perfil.ADMINISTRADOR
                && usuario.getPerfil() != Perfil.ADMINISTRADOR;
        if (perdeAdministrador && usuario.getId() == idAdministradorLogado) {
            throw new RegraNegocioException(
                    "Não é possível retirar o perfil Administrador do próprio usuário logado.");
        }
        if (perdeAdministrador && usuarioDAO.contarAdministradores() <= 1) {
            throw new RegraNegocioException(
                    "Este é o único Administrador cadastrado; o perfil não pode ser alterado.");
        }

        boolean informouSenha = novaSenha != null && novaSenha.length > 0;
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {
            if (!informouSenha && atual.getSenhaHash() == null) {
                throw new RegraNegocioException(
                        "O perfil Administrador exige a definição de uma senha.");
            }
            usuario.setSenhaHash(informouSenha ? Senhas.gerarHash(novaSenha) : atual.getSenhaHash());
        } else {
            usuario.setLogin(null);
            usuario.setSenhaHash(null);
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
        if (semCpf) {
            u.setCpf(null);
        }
        if (semMatricula) {
            u.setMatricula(null);
        }
        if (u.getLogin() != null && u.getLogin().isBlank()) {
            u.setLogin(null);
        }
        if (u.getPerfil() == Perfil.ADMINISTRADOR && u.getLogin() == null) {
            throw new RegraNegocioException("Informe o nome de usuário do Administrador.");
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
