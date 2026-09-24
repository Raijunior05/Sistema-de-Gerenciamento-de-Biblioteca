package br.univasf.bibliotech.dao;

import br.univasf.bibliotech.exception.DataAccessException;
import br.univasf.bibliotech.model.Emprestimo;
import br.univasf.bibliotech.model.Item;
import br.univasf.bibliotech.model.StatusEmprestimo;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.service.EmprestimoService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CU 10 passo 07 e CU 12 fluxo 5.1: migracao e persistencia de atraso sem cobranca. */
@Testcontainers
@Tag("integracao")
class RemocaoMultasIntegracaoTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    @DisplayName("CU 12, fluxo 5.1: V4 preserva historico e CU 10 bloqueia apenas atraso aberto")
    void deveMigrarHistoricoEManterSomentePendenciaDeAtraso() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        try (HikariDataSource ds = new HikariDataSource(config)) {
            Flyway.configure().dataSource(ds).locations("classpath:db/migration").target("3").load().migrate();
            UsuarioDAO usuarios = new UsuarioDAOPostgres(ds);
            ItemDAO itens = new ItemDAOPostgres(ds);
            EmprestimoDAO emprestimos = new EmprestimoDAOPostgres(ds);
            ReservaDAO reservas = new ReservaDAOPostgres(ds);
            Usuario usuario = new Usuario();
            usuario.setNome("Leitor de teste");
            usuario.setEmail("leitor@teste.local");
            usuario.setMatricula("M001");
            usuarios.inserir(usuario);
            Item item = new Item();
            item.setTitulo("Livro de teste");
            item.setAutor("Autor");
            item.setQuantidadeTotal(3);
            item.setQuantidadeDisponivel(1);
            itens.inserir(item);

            Emprestimo historico = new Emprestimo(usuario, item, LocalDate.now().minusDays(6));
            historico.setDataEmprestimo(LocalDate.now().minusDays(20));
            // O INSERT do DAO cria apenas emprestimos abertos; concluir em seguida.
            historico.setStatus(StatusEmprestimo.EM_ANDAMENTO);
            emprestimos.inserir(historico);
            historico.setDataDevolucao(LocalDate.now().minusDays(2));
            historico.setStatus(StatusEmprestimo.CONCLUIDO);
            historico.setDiasAtraso(4);
            emprestimos.atualizar(historico);
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE emprestimo SET valor_multa = ?, multa_paga = FALSE WHERE id = ?")) {
                ps.setInt(1, 6);
                ps.setLong(2, historico.getId());
                ps.executeUpdate();
            }
            Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
            Emprestimo lido = emprestimos.buscarPorId(historico.getId()).orElseThrow();
            assertEquals(4, lido.getDiasAtraso());
            assertEquals(historico.getDataDevolucao(), lido.getDataDevolucao());
            assertEquals(usuario.getId(), lido.getUsuario().getId());
            assertFalse(emprestimos.possuiPendencia(usuario.getId()));
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'public' AND table_name = 'emprestimo'
                           AND column_name IN ('valor_multa', 'multa_paga')
                         """);
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1));
            }
            Emprestimo aberto = new Emprestimo(usuario, item, LocalDate.now().minusDays(3));
            aberto.setDataEmprestimo(LocalDate.now().minusDays(18));
            emprestimos.inserir(aberto);
            // Mesmo sem atualizar o status no start, a data vencida deve bloquear.
            assertTrue(emprestimos.possuiPendencia(usuario.getId()));
            EmprestimoService service = new EmprestimoService(emprestimos, itens, reservas,
                    new TransacaoJdbc(ds), 3, 15, 7);
            service.registrarDevolucao(aberto);
            assertFalse(emprestimos.possuiPendencia(usuario.getId()));
            assertEquals(3, emprestimos.buscarPorId(aberto.getId()).orElseThrow().getDiasAtraso());
            Emprestimo novo = service.registrar(usuario, item, null);
            assertEquals(StatusEmprestimo.EM_ANDAMENTO, novo.getStatus());
            assertFalse(emprestimos.possuiPendencia(usuario.getId()));
            lido.setDiasAtraso(-1);
            assertThrows(DataAccessException.class, () -> emprestimos.atualizar(lido));
        }
    }
}
