package br.univasf.bibliotech.dao;

import br.univasf.bibliotech.model.Item;
import br.univasf.bibliotech.model.Perfil;
import br.univasf.bibliotech.model.Usuario;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integracao contra um PostgreSQL real, criado e destruido pelo
 * Testcontainers.
 *
 * <p>Complementa os testes com Mockito: aqui o que se valida e o SQL, o
 * mapeamento ResultSet para objeto e as constraints escritas nas migrations.
 * Um mock jamais pegaria um nome de coluna errado.</p>
 *
 * <p>Exige Docker em execucao. Marcado com a tag "integracao" e excluido da
 * suite padrao; rode com {@code mvn test -Dgroups=integracao -DgruposExcluidos=}.
 * Com Docker 29, acrescente {@code -Dapi.version=1.44}.</p>
 */
@Testcontainers
@Tag("integracao")
@DisplayName("DAOs contra PostgreSQL real")
class DaoIntegracaoTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("bibliotech_test")
                    .withUsername("teste")
                    .withPassword("teste");

    private static DataSource dataSource;
    private static UsuarioDAO usuarioDAO;
    private static ItemDAO itemDAO;

    @BeforeAll
    static void prepararBanco() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(POSTGRES.getJdbcUrl());
        cfg.setUsername(POSTGRES.getUsername());
        cfg.setPassword(POSTGRES.getPassword());
        cfg.setMaximumPoolSize(3);
        dataSource = new HikariDataSource(cfg);

        // As mesmas migrations da aplicacao montam o schema de teste.
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        usuarioDAO = new UsuarioDAOPostgres(dataSource);
        itemDAO = new ItemDAOPostgres(dataSource);
    }

    @AfterAll
    static void encerrar() {
        if (dataSource instanceof HikariDataSource hds) {
            hds.close();
        }
    }

    @Test
    @DisplayName("insere e recupera usuario, validando o mapeamento das colunas")
    void deveInserirERecuperarUsuario() {
        Usuario u = new Usuario();
        u.setNome("Juliana Reis Mendes");
        u.setEmail("juliana." + System.nanoTime() + "@email.com");
        u.setCpf("333." + (System.nanoTime() % 1000) + ".333-33");
        u.setTelefone("(87) 99911-0003");
        u.setPerfil(Perfil.USUARIO);

        usuarioDAO.inserir(u);
        assertNotNull(u.getId());

        Usuario lido = usuarioDAO.buscarPorId(u.getId()).orElseThrow();
        assertEquals("Juliana Reis Mendes", lido.getNome());
        assertEquals(u.getCpf(), lido.getCpf());
        assertEquals(Perfil.USUARIO, lido.getPerfil());
        assertTrue(lido.isAtivo());
    }

    @Test
    @DisplayName("constraint chk_usuario_identificacao bloqueia usuario sem CPF e sem matricula")
    void deveBloquearUsuarioSemIdentificacao() {
        Usuario u = new Usuario();
        u.setNome("Sem identificacao");
        u.setEmail("sem.id." + System.nanoTime() + "@email.com");
        u.setPerfil(Perfil.USUARIO);

        // A regra tambem existe no UsuarioService, mas o banco e a ultima linha
        // de defesa: mesmo um INSERT direto precisa ser recusado.
        assertThrows(RuntimeException.class, () -> usuarioDAO.inserir(u));
    }

    @Test
    @DisplayName("gera tombo sequencial e controla a disponibilidade do item")
    void deveControlarDisponibilidadeDoItem() {
        Item item = new Item();
        item.setTitulo("Dom Casmurro");
        item.setAutor("Machado de Assis");
        item.setIsbn("ISBN-" + System.nanoTime());
        item.setQuantidadeTotal(2);

        itemDAO.inserir(item);

        assertNotNull(item.getTombo());
        assertTrue(item.getTombo().startsWith("ACV-"));

        // CU 7: item novo entra com todos os exemplares disponiveis
        item.setQuantidadeDisponivel(2);
        itemDAO.atualizar(item);
        assertEquals(2, itemDAO.quantidadeDisponivel(item.getId()));

        // CU 10 passo 09
        assertTrue(itemDAO.decrementarDisponivel(item.getId()));
        assertTrue(itemDAO.decrementarDisponivel(item.getId()));
        assertEquals(0, itemDAO.quantidadeDisponivel(item.getId()));

        // Fluxo 3.1: sem exemplar, o decremento falha em vez de ficar negativo
        assertFalse(itemDAO.decrementarDisponivel(item.getId()));

        // CU 12 passo 07
        itemDAO.incrementarDisponivel(item.getId());
        assertEquals(1, itemDAO.quantidadeDisponivel(item.getId()));
    }

    @Test
    @DisplayName("pesquisa de usuario encontra por nome parcial")
    void devePesquisarPorNomeParcial() {
        Usuario u = new Usuario();
        u.setNome("Fernanda Rocha Dias");
        u.setEmail("fernanda." + System.nanoTime() + "@email.com");
        u.setMatricula("MAT" + System.nanoTime());
        u.setPerfil(Perfil.USUARIO);
        usuarioDAO.inserir(u);

        assertFalse(usuarioDAO.pesquisar("Rocha").isEmpty());
    }
}
