package br.univasf.bibliotech.service;

import br.univasf.bibliotech.dao.UsuarioDAO;
import br.univasf.bibliotech.exception.DadosDuplicadosException;
import br.univasf.bibliotech.exception.ExclusaoNaoPermitidaException;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.exception.UsuarioNaoEncontradoException;
import br.univasf.bibliotech.model.Perfil;
import br.univasf.bibliotech.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Casos de Uso 3, 5, 6 e 9. */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - CU 3 a 6 e CU 9")
class UsuarioServiceTest {

    private static final long ID_ADMIN_LOGADO = 99L;

    @Mock private UsuarioDAO usuarioDAO;

    private UsuarioService service;

    @BeforeEach
    void preparar() {
        service = new UsuarioService(usuarioDAO);
    }

    private Usuario usuarioValido() {
        Usuario u = new Usuario();
        u.setNome("Mariana Costa Silva");
        u.setEmail("mariana.costa@email.com");
        u.setCpf("222.222.222-22");
        u.setPerfil(Perfil.USUARIO);
        return u;
    }

    @Nested
    @DisplayName("CU 3 - Cadastrar Usuario")
    class Cadastrar {

        @Test
        @DisplayName("fluxo principal: salva o usuario comum sem credenciais")
        void deveCadastrarUsuarioComum() {
            Usuario u = usuarioValido();

            service.cadastrar(u, null);

            // Usuario comum nao acessa o sistema (CU 1)
            assertNull(u.getSenhaHash());
            assertNull(u.getLogin());
            verify(usuarioDAO).inserir(u);
        }

        @Test
        @DisplayName("perfil Administrador tem a senha armazenada com hash BCrypt")
        void deveGerarHashParaAdministrador() {
            Usuario u = usuarioValido();
            u.setPerfil(Perfil.ADMINISTRADOR);
            u.setLogin("mariana");

            service.cadastrar(u, "senhaForte123".toCharArray());

            assertNotNull(u.getSenhaHash());
            assertEquals(60, u.getSenhaHash().length());
            // RNF-05: a senha nunca fica em texto puro
            assertEquals(false, u.getSenhaHash().contains("senhaForte123"));
        }

        @Test
        @DisplayName("perfil Administrador sem senha e recusado")
        void deveExigirSenhaParaAdministrador() {
            Usuario u = usuarioValido();
            u.setPerfil(Perfil.ADMINISTRADOR);

            assertThrows(RegraNegocioException.class, () -> service.cadastrar(u, null));
            verify(usuarioDAO, never()).inserir(any());
        }

        @Test
        @DisplayName("fluxo 4.1: e-mail ja cadastrado")
        void deveRecusarEmailDuplicado() {
            Usuario u = usuarioValido();
            when(usuarioDAO.existeEmail(u.getEmail(), null)).thenReturn(true);

            DadosDuplicadosException erro = assertThrows(DadosDuplicadosException.class,
                    () -> service.cadastrar(u, null));

            assertEquals("e-mail", erro.getCampo());
            verify(usuarioDAO, never()).inserir(any());
        }

        @Test
        @DisplayName("fluxo 4.1: sem CPF nem matricula o cadastro e bloqueado")
        void deveExigirCpfOuMatricula() {
            Usuario u = usuarioValido();
            u.setCpf(null);
            u.setMatricula(null);

            assertThrows(RegraNegocioException.class, () -> service.cadastrar(u, null));
        }

        @Test
        @DisplayName("fluxo 4.1: campo obrigatorio vazio")
        void deveExigirNome() {
            Usuario u = usuarioValido();
            u.setNome("  ");

            assertThrows(RegraNegocioException.class, () -> service.cadastrar(u, null));
        }
    }

    @Nested
    @DisplayName("CU 5 - Excluir Usuario, fluxo 4.1")
    class Excluir {

        @Test
        @DisplayName("bloqueia quando ha emprestimo ou reserva ativa")
        void deveBloquearComPendencia() {
            Usuario alvo = usuarioValido();
            alvo.setId(1L);
            when(usuarioDAO.buscarPorId(1L)).thenReturn(Optional.of(alvo));
            when(usuarioDAO.possuiEmprestimoOuReservaAtiva(1L)).thenReturn(true);

            assertThrows(ExclusaoNaoPermitidaException.class,
                    () -> service.excluir(1L, ID_ADMIN_LOGADO));

            verify(usuarioDAO, never()).excluir(anyLong());
        }

        @Test
        @DisplayName("bloqueia a auto-exclusao do administrador logado")
        void deveBloquearAutoExclusao() {
            Usuario alvo = usuarioValido();
            alvo.setId(ID_ADMIN_LOGADO);
            when(usuarioDAO.buscarPorId(ID_ADMIN_LOGADO)).thenReturn(Optional.of(alvo));

            assertThrows(ExclusaoNaoPermitidaException.class,
                    () -> service.excluir(ID_ADMIN_LOGADO, ID_ADMIN_LOGADO));

            verify(usuarioDAO, never()).excluir(anyLong());
        }

        @Test
        @DisplayName("bloqueia a remocao do unico administrador")
        void deveBloquearUltimoAdministrador() {
            Usuario alvo = usuarioValido();
            alvo.setId(5L);
            alvo.setPerfil(Perfil.ADMINISTRADOR);
            when(usuarioDAO.buscarPorId(5L)).thenReturn(Optional.of(alvo));
            when(usuarioDAO.possuiEmprestimoOuReservaAtiva(5L)).thenReturn(false);
            when(usuarioDAO.contarAdministradores()).thenReturn(1L);

            assertThrows(ExclusaoNaoPermitidaException.class,
                    () -> service.excluir(5L, ID_ADMIN_LOGADO));
        }

        @Test
        @DisplayName("fluxo principal: remove quando nao ha impedimento")
        void deveExcluirSemPendencia() {
            Usuario alvo = usuarioValido();
            alvo.setId(1L);
            when(usuarioDAO.buscarPorId(1L)).thenReturn(Optional.of(alvo));
            when(usuarioDAO.possuiEmprestimoOuReservaAtiva(1L)).thenReturn(false);

            service.excluir(1L, ID_ADMIN_LOGADO);

            verify(usuarioDAO).excluir(1L);
        }
    }

    @Nested
    @DisplayName("CU 9 - Identificar Usuario")
    class Identificar {

        @Test
        @DisplayName("fluxo principal: localiza por CPF ou matricula")
        void deveLocalizarPorIdentificacao() {
            Usuario esperado = usuarioValido();
            esperado.setId(1L);
            when(usuarioDAO.buscarPorIdentificacao("222.222.222-22"))
                    .thenReturn(Optional.of(esperado));

            Usuario obtido = service.identificar("222.222.222-22");

            assertEquals(esperado, obtido);
        }

        @Test
        @DisplayName("fluxo 4.1: identificacao inexistente")
        void deveFalharQuandoNaoEncontra() {
            when(usuarioDAO.buscarPorIdentificacao("999")).thenReturn(Optional.empty());

            UsuarioNaoEncontradoException erro = assertThrows(
                    UsuarioNaoEncontradoException.class, () -> service.identificar("999"));

            assertEquals("999", erro.getIdentificacao());
        }
    }
}
