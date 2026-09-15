package br.univasf.bibliotech.dao;

import br.univasf.bibliotech.exception.DataAccessException;
import br.univasf.bibliotech.model.Emprestimo;
import br.univasf.bibliotech.model.StatusEmprestimo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CU 10 passos 07 e 08, CU 12 passos 05 e 06 e CU 13: persistencia e consulta de emprestimos. */
public class EmprestimoDAOPostgres implements EmprestimoDAO {

    /**
     * O JOIN traz usuario e item junto para evitar N+1 consultas ao montar as
     * tabelas de consulta de emprestimos na area de Relatorios.
     */
    private static final String SELECT_BASE = """
            SELECT e.id, e.codigo, e.data_emprestimo, e.data_prevista, e.data_devolucao,
                   e.status, e.dias_atraso,
                   u.id AS u_id, u.nome AS u_nome, u.email AS u_email,
                   u.cpf AS u_cpf, u.matricula AS u_matricula,
                   i.id AS i_id, i.titulo AS i_titulo, i.autor AS i_autor,
                   i.tombo AS i_tombo, i.quantidade_disp AS i_disp,
                   i.quantidade_total AS i_total
              FROM emprestimo e
              JOIN usuario u ON u.id = e.usuario_id
              JOIN item    i ON i.id = e.item_id
            """;

    private final DataSource dataSource;

    public EmprestimoDAOPostgres(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void inserir(Emprestimo e) {
        if (e.getCodigo() == null || e.getCodigo().isBlank()) {
            e.setCodigo(gerarProximoCodigo());
        }
        String sql = """
                INSERT INTO emprestimo
                    (codigo, usuario_id, item_id, data_emprestimo, data_prevista,
                     status, registrado_por)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, e.getCodigo());
            ps.setLong(2, e.getUsuario().getId());
            ps.setLong(3, e.getItem().getId());
            ps.setDate(4, Date.valueOf(e.getDataEmprestimo()));
            ps.setDate(5, Date.valueOf(e.getDataPrevista()));
            ps.setString(6, e.getStatus().name());
            if (e.getRegistradoPor() == null) {
                ps.setNull(7, Types.BIGINT);
            } else {
                ps.setLong(7, e.getRegistradoPor().getId());
            }
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    e.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao registrar emprestimo.", ex);
        }
    }

    @Override
    public void atualizar(Emprestimo e) {
        String sql = """
                UPDATE emprestimo SET
                    data_devolucao = ?, status = ?, dias_atraso = ?
                WHERE id = ?
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setDate(1, e.getDataDevolucao() == null ? null : Date.valueOf(e.getDataDevolucao()));
            ps.setString(2, e.getStatus().name());
            ps.setInt(3, e.getDiasAtraso());
            ps.setLong(4, e.getId());
            ps.executeUpdate();

        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao atualizar emprestimo.", ex);
        }
    }

    @Override
    public Optional<Emprestimo> buscarPorId(long id) {
        List<Emprestimo> l = buscarLista(SELECT_BASE + " WHERE e.id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public Optional<Emprestimo> buscarPorCodigo(String codigo) {
        List<Emprestimo> l = buscarLista(SELECT_BASE + " WHERE e.codigo = ?", codigo);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public List<Emprestimo> listarAbertosPorUsuario(long usuarioId) {
        return buscarLista(SELECT_BASE
                + " WHERE e.usuario_id = ? AND e.status <> 'CONCLUIDO'"
                + " ORDER BY e.data_prevista", usuarioId);
    }

    @Override
    public List<Emprestimo> listarPorUsuario(long usuarioId) {
        return buscarLista(SELECT_BASE
                + " WHERE e.usuario_id = ? ORDER BY e.data_emprestimo DESC", usuarioId);
    }

    @Override
    public int contarAtivosPorUsuario(long usuarioId) {
        String sql = "SELECT COUNT(*) FROM emprestimo WHERE usuario_id = ? AND status <> 'CONCLUIDO'";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao contar emprestimos ativos.", ex);
        }
    }

    @Override
    public boolean possuiPendencia(long usuarioId) {
        String sql = """
                SELECT 1 FROM emprestimo
                 WHERE usuario_id = ?
                   AND status <> 'CONCLUIDO' AND data_prevista < CURRENT_DATE
                 LIMIT 1
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao verificar pendencias.", ex);
        }
    }

    @Override
    public List<Emprestimo> pesquisar(String termo, StatusEmprestimo status,
                                      LocalDate inicio, LocalDate fim) {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (termo != null && !termo.isBlank()) {
            sql.append("""
                     AND (LOWER(u.nome) LIKE LOWER(?)
                          OR LOWER(i.titulo) LIKE LOWER(?)
                          OR e.codigo LIKE ?)
                    """);
            String like = "%" + termo.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (status != null) {
            sql.append(" AND e.status = ?");
            params.add(status.name());
        }
        if (inicio != null) {
            sql.append(" AND e.data_emprestimo >= ?");
            params.add(Date.valueOf(inicio));
        }
        if (fim != null) {
            sql.append(" AND e.data_emprestimo <= ?");
            params.add(Date.valueOf(fim));
        }
        sql.append(" ORDER BY e.data_emprestimo DESC");

        return buscarLista(sql.toString(), params.toArray());
    }

    @Override
    public List<Emprestimo> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return pesquisar(null, null, inicio, fim);
    }

    @Override
    public int contarPorStatus(StatusEmprestimo status) {
        String sql = "SELECT COUNT(*) FROM emprestimo WHERE status = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao contar emprestimos por status.", ex);
        }
    }

    @Override
    public int marcarAtrasados(LocalDate referencia) {
        String sql = """
                UPDATE emprestimo
                   SET status = 'ATRASADO',
                       dias_atraso = (? - data_prevista)
                 WHERE status = 'EM_ANDAMENTO' AND data_prevista < ?
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(referencia));
            ps.setDate(2, Date.valueOf(referencia));
            return ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao atualizar emprestimos atrasados.", ex);
        }
    }

    @Override
    public String gerarProximoCodigo() {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo FROM 5) AS INTEGER)), 1000) + 1 "
                + "FROM emprestimo WHERE codigo LIKE 'EMP-%'";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int proximo = rs.next() ? rs.getInt(1) : 1001;
            return String.format("EMP-%04d", proximo);
        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao gerar codigo de emprestimo.", ex);
        }
    }

    private List<Emprestimo> buscarLista(String sql, Object... params) {
        List<Emprestimo> lista = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;

        } catch (SQLException ex) {
            throw new DataAccessException("Falha ao consultar emprestimos.", ex);
        }
    }

    private Emprestimo mapear(ResultSet rs) throws SQLException {
        Emprestimo e = new Emprestimo();
        e.setId(rs.getLong("id"));
        e.setCodigo(rs.getString("codigo"));
        e.setDataEmprestimo(rs.getDate("data_emprestimo").toLocalDate());
        e.setDataPrevista(rs.getDate("data_prevista").toLocalDate());

        Date dev = rs.getDate("data_devolucao");
        e.setDataDevolucao(dev == null ? null : dev.toLocalDate());

        e.setStatus(StatusEmprestimo.valueOf(rs.getString("status")));
        e.setDiasAtraso(rs.getInt("dias_atraso"));

        var u = new br.univasf.bibliotech.model.Usuario();
        u.setId(rs.getLong("u_id"));
        u.setNome(rs.getString("u_nome"));
        u.setEmail(rs.getString("u_email"));
        u.setCpf(rs.getString("u_cpf"));
        u.setMatricula(rs.getString("u_matricula"));
        e.setUsuario(u);

        var i = new br.univasf.bibliotech.model.Item();
        i.setId(rs.getLong("i_id"));
        i.setTitulo(rs.getString("i_titulo"));
        i.setAutor(rs.getString("i_autor"));
        i.setTombo(rs.getString("i_tombo"));
        i.setQuantidadeDisponivel(rs.getInt("i_disp"));
        i.setQuantidadeTotal(rs.getInt("i_total"));
        e.setItem(i);

        return e;
    }
}
