package br.univasf.bibliotech.dao;

import br.univasf.bibliotech.exception.DataAccessException;
import br.univasf.bibliotech.model.Item;
import br.univasf.bibliotech.model.Reserva;
import br.univasf.bibliotech.model.StatusReserva;
import br.univasf.bibliotech.model.Usuario;

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

public class ReservaDAOPostgres implements ReservaDAO {

    private static final String SELECT_BASE = """
            SELECT r.id, r.data_reserva, r.validade_maxima, r.posicao_fila, r.status,
                   u.id AS u_id, u.nome AS u_nome, u.email AS u_email,
                   u.cpf AS u_cpf, u.matricula AS u_matricula,
                   i.id AS i_id, i.titulo AS i_titulo, i.autor AS i_autor,
                   i.tombo AS i_tombo, i.quantidade_disp AS i_disp,
                   i.quantidade_total AS i_total
              FROM reserva r
              LEFT JOIN usuario u ON u.id = r.usuario_id
              JOIN item         i ON i.id = r.item_id
            """;

    private final DataSource dataSource;

    public ReservaDAOPostgres(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void inserir(Reserva r) {
        String sql = """
                INSERT INTO reserva
                    (usuario_id, item_id, data_reserva, validade_maxima,
                     posicao_fila, status, registrado_por)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, r.getUsuario().getId());
            ps.setLong(2, r.getItem().getId());
            ps.setDate(3, Date.valueOf(r.getDataReserva()));
            ps.setDate(4, Date.valueOf(r.getValidadeMaxima()));
            ps.setInt(5, r.getPosicaoFila());
            ps.setString(6, r.getStatus().name());
            if (r.getRegistradoPor() == null) {
                ps.setNull(7, Types.BIGINT);
            } else {
                ps.setLong(7, r.getRegistradoPor().getId());
            }
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    r.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao registrar reserva.", e);
        }
    }

    @Override
    public void atualizar(Reserva r) {
        String sql = "UPDATE reserva SET status = ?, posicao_fila = ?, validade_maxima = ? WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getStatus().name());
            ps.setInt(2, r.getPosicaoFila());
            ps.setDate(3, Date.valueOf(r.getValidadeMaxima()));
            ps.setLong(4, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao atualizar reserva.", e);
        }
    }

    @Override
    public Optional<Reserva> buscarPorId(long id) {
        List<Reserva> l = buscarLista(SELECT_BASE + " WHERE r.id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public boolean possuiReservaAtiva(long usuarioId, long itemId) {
        String sql = """
                SELECT 1 FROM reserva
                 WHERE usuario_id = ? AND item_id = ?
                   AND status IN ('AGUARDANDO', 'DISPONIVEL')
                 LIMIT 1
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            ps.setLong(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao verificar reserva duplicada.", e);
        }
    }

    @Override
    public int proximaPosicaoFila(long itemId) {
        String sql = """
                SELECT COALESCE(MAX(posicao_fila), 0) + 1
                  FROM reserva
                 WHERE item_id = ? AND status IN ('AGUARDANDO', 'DISPONIVEL')
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao calcular posicao na fila.", e);
        }
    }

    @Override
    public Optional<Reserva> primeiroDaFila(long itemId) {
        String sql = SELECT_BASE + """
                 WHERE r.item_id = ? AND r.status = 'AGUARDANDO'
                 ORDER BY r.posicao_fila
                 LIMIT 1
                """;
        List<Reserva> l = buscarLista(sql, itemId);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public List<Reserva> pesquisar(String termo, StatusReserva status, boolean ordemCrescente) {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (termo != null && !termo.isBlank()) {
            sql.append(" AND (LOWER(u.nome) LIKE LOWER(?) OR LOWER(i.titulo) LIKE LOWER(?))");
            String like = "%" + termo.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (status != null) {
            sql.append(" AND r.status = ?");
            params.add(status.name());
        }
        sql.append(" ORDER BY r.posicao_fila ").append(ordemCrescente ? "ASC" : "DESC");

        return buscarLista(sql.toString(), params.toArray());
    }

    @Override
    public List<Reserva> listarPorItem(long itemId) {
        return buscarLista(SELECT_BASE + " WHERE r.item_id = ? ORDER BY r.posicao_fila", itemId);
    }

    @Override
    public List<Reserva> listarPorUsuario(long usuarioId) {
        return buscarLista(SELECT_BASE + " WHERE r.usuario_id = ? ORDER BY r.data_reserva DESC",
                usuarioId);
    }

    @Override
    public int contarPorStatus(StatusReserva status) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM reserva WHERE status = ?")) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao contar reservas.", e);
        }
    }

    @Override
    public int expirarVencidas(LocalDate referencia) {
        String sql = """
                UPDATE reserva SET status = 'EXPIRADA'
                 WHERE status IN ('AGUARDANDO', 'DISPONIVEL')
                   AND validade_maxima < ?
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(referencia));
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao expirar reservas vencidas.", e);
        }
    }

    private List<Reserva> buscarLista(String sql, Object... params) {
        List<Reserva> lista = new ArrayList<>();
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

        } catch (SQLException e) {
            throw new DataAccessException("Falha ao consultar reservas.", e);
        }
    }

    private Reserva mapear(ResultSet rs) throws SQLException {
        Reserva r = new Reserva();
        r.setId(rs.getLong("id"));
        r.setDataReserva(rs.getDate("data_reserva").toLocalDate());
        r.setValidadeMaxima(rs.getDate("validade_maxima").toLocalDate());
        r.setPosicaoFila(rs.getInt("posicao_fila"));
        r.setStatus(StatusReserva.valueOf(rs.getString("status")));

        Usuario u = new Usuario();
        u.setId(rs.getObject("u_id", Long.class));
        u.setNome(u.getId() == null ? EmprestimoDAOPostgres.USUARIO_EXCLUIDO : rs.getString("u_nome"));
        u.setEmail(rs.getString("u_email"));
        u.setCpf(rs.getString("u_cpf"));
        u.setMatricula(rs.getString("u_matricula"));
        r.setUsuario(u);

        Item i = new Item();
        i.setId(rs.getLong("i_id"));
        i.setTitulo(rs.getString("i_titulo"));
        i.setAutor(rs.getString("i_autor"));
        i.setTombo(rs.getString("i_tombo"));
        i.setQuantidadeDisponivel(rs.getInt("i_disp"));
        i.setQuantidadeTotal(rs.getInt("i_total"));
        r.setItem(i);

        return r;
    }
}
