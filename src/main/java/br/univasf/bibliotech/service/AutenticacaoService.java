package br.univasf.bibliotech.service;

import br.univasf.bibliotech.dao.UsuarioDAO;
import br.univasf.bibliotech.exception.CredenciaisInvalidasException;
import br.univasf.bibliotech.model.Perfil;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.util.Senhas;

import java.util.Optional;

/**
 * Caso de Uso 1 - Validar Usuario.
 *
 * <p>O prototipo faz login por e-mail; a especificacao fala em nome de
 * usuario. O metodo {@link #autenticar} aceita os dois, o que satisfaz as
 * duas fontes sem exigir campo adicional na tela.</p>
 */
public class AutenticacaoService {

    private final UsuarioDAO usuarioDAO;

    public AutenticacaoService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    /**
     * Fluxo principal, passos 03 e 04.
     *
     * @param emailOuLogin credencial digitada na tela de login
     * @param senha        senha em texto puro
     * @return o Administrador autenticado
     * @throws CredenciaisInvalidasException fluxo alternativo 3.1
     */
    public Usuario autenticar(String emailOuLogin, char[] senha) {
        if (emailOuLogin == null || emailOuLogin.isBlank() || senha == null || senha.length == 0) {
            throw new CredenciaisInvalidasException();
        }

        Optional<Usuario> encontrado = usuarioDAO.buscarPorCredencial(emailOuLogin.trim());

        // Verifica o hash mesmo quando o usuario nao existe, para que o tempo
        // de resposta nao revele quais credenciais estao cadastradas.
        String hash = encontrado.map(Usuario::getSenhaHash).orElse(null);
        boolean confere = Senhas.conferem(senha, hash);

        if (encontrado.isEmpty() || !confere) {
            throw new CredenciaisInvalidasException();
        }

        Usuario usuario = encontrado.get();
        if (usuario.getPerfil() != Perfil.ADMINISTRADOR || !usuario.isAtivo()) {
            throw new CredenciaisInvalidasException();
        }
        return usuario;
    }

    /**
     * Garante que exista pelo menos um Administrador no sistema.
     *
     * <p>Resolve a dependencia circular entre o CU 1 (exige estar logado) e o
     * CU 3 (exige um Administrador para cadastrar outro). Executado no start
     * da aplicacao; nao faz nada se ja houver Administrador.</p>
     *
     * @return o administrador criado, ou vazio se ja existia algum
     */
    public Optional<Usuario> garantirAdministradorInicial(String senhaPadrao) {
        if (usuarioDAO.contarAdministradores() > 0) {
            return Optional.empty();
        }

        Usuario admin = new Usuario();
        admin.setNome("Administrador do sistema");
        admin.setEmail("admin@bibliotech.local");
        admin.setCpf("000.000.000-00");
        admin.setLogin("admin");
        admin.setPerfil(Perfil.ADMINISTRADOR);
        admin.setSenhaHash(Senhas.gerarHash(senhaPadrao));

        usuarioDAO.inserir(admin);
        return Optional.of(admin);
    }
}
