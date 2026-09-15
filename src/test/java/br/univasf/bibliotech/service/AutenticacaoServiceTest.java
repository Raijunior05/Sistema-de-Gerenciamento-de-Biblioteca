package br.univasf.bibliotech.service;

import br.univasf.bibliotech.dao.UsuarioDAO;
import br.univasf.bibliotech.exception.CredenciaisInvalidasException;
import br.univasf.bibliotech.model.Perfil;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.util.Senhas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Caso de Uso 1 - Validar Usuario. */
@ExtendWith(MockitoExtension.class)
@DisplayName("AutenticacaoService - CU 1")
class AutenticacaoServiceTest {

    private static final String SENHA_CORRETA = "admin123";

    @Mock private UsuarioDAO usuarioDAO;

    private AutenticacaoService service;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        service = new AutenticacaoService(usuarioDAO);

        admin = new Usuario(1L, "Administrador do sistema");
        admin.setEmail("admin@bibliotech.local");
        admin.setLogin("admin");
        admin.setPerfil(Perfil.ADMINISTRADOR);
        admin.setSenhaHash(Senhas.gerarHash(SENHA_CORRETA));
    }

    @Test
    @DisplayName("fluxo principal: credenciais corretas liberam o acesso")
    void deveAutenticarComCredenciaisValidas() {
        when(usuarioDAO.buscarPorCredencial("admin@bibliotech.local"))
                .thenReturn(Optional.of(admin));

        Usuario logado = service.autenticar("admin@bibliotech.local",
                SENHA_CORRETA.toCharArray());

        assertEquals(admin, logado);
    }

    @Test
    @DisplayName("aceita tambem o nome de usuario, conforme a especificacao")
    void deveAutenticarPorLogin() {
        when(usuarioDAO.buscarPorCredencial("admin")).thenReturn(Optional.of(admin));

        assertEquals(admin, service.autenticar("admin", SENHA_CORRETA.toCharArray()));
    }

    @Test
    @DisplayName("fluxo 3.1: senha incorreta")
    void deveRecusarSenhaIncorreta() {
        when(usuarioDAO.buscarPorCredencial("admin")).thenReturn(Optional.of(admin));

        assertThrows(CredenciaisInvalidasException.class,
                () -> service.autenticar("admin", "errada".toCharArray()));
    }

    @Test
    @DisplayName("fluxo 3.1: usuario inexistente")
    void deveRecusarUsuarioInexistente() {
        when(usuarioDAO.buscarPorCredencial("ninguem")).thenReturn(Optional.empty());

        assertThrows(CredenciaisInvalidasException.class,
                () -> service.autenticar("ninguem", SENHA_CORRETA.toCharArray()));
    }

    @Test
    @DisplayName("campos vazios nao chegam a consultar o banco")
    void deveRecusarCamposVazios() {
        assertThrows(CredenciaisInvalidasException.class,
                () -> service.autenticar("", new char[0]));

        verify(usuarioDAO, never()).buscarPorCredencial(any());
    }

    @Test
    @DisplayName("cria o administrador inicial apenas quando nao existe nenhum")
    void deveCriarAdministradorInicialSomenteUmaVez() {
        when(usuarioDAO.contarAdministradores()).thenReturn(0L);

        Optional<Usuario> criado = service.garantirAdministradorInicial(SENHA_CORRETA);

        assertTrue(criado.isPresent());
        assertTrue(Senhas.conferem(SENHA_CORRETA, criado.get().getSenhaHash()));
        verify(usuarioDAO).inserir(any(Usuario.class));
    }

    @Test
    @DisplayName("nao recria o administrador se ja houver um cadastrado")
    void naoDeveRecriarAdministrador() {
        when(usuarioDAO.contarAdministradores()).thenReturn(2L);

        assertTrue(service.garantirAdministradorInicial(SENHA_CORRETA).isEmpty());
        verify(usuarioDAO, never()).inserir(any());
    }
}
