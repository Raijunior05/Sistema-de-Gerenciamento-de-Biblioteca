package br.univasf.bibliotech.dao;

import br.univasf.bibliotech.exception.DataAccessException;
import br.univasf.bibliotech.model.Item;
import br.univasf.bibliotech.model.TipoItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDAOPostgres implements ItemDAO {

    private static final String COLUNAS = """
            id, tombo, titulo, autor, isbn, editora, ano_publicacao,
            categoria, tipo, quantidade_total, quantidade_disp, criado_em
            """;

    private final DataSource dataSource;

    public ItemDAOPostgres(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void inserir(Item item) {
        if (item.getTombo() == null || item.getTombo().isBlank()) {
            item.setTombo(gerarProximoTombo());
        }
        String sql = """
                INSERT INTO item
                    (tombo, titulo, autor, isbn, editora, ano_publicacao,
                     categoria, tipo, quantidade_total, quantidade_disp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preencher(ps, item);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    item.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao inserir item no acervo.", e);
        }
    }

    @Override
    public void atualizar(Item item) {
        String sql = """
                UPDATE item SET
                    tombo = ?, titulo = ?, autor = ?, isbn = ?, editora = ?,
                    ano_publicacao = ?, categoria = ?, tipo = ?,
                    quantidade_total = ?, quantidade_disp = ?
                WHERE id = ?
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            preencher(ps, item);
            ps.setLong(11, item.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Falha ao atualizar item.", e);
        }
    }

    private void preencher(PreparedStatement ps, Item i) throws SQLException {
        ps.setString(1, i.getTombo());
        ps.setString(2, i.getTitulo());
        ps.setString(3, i.getAutor());
        ps.setString(4, i.getIsbn());
        ps.setString(5, i.getEditora());
        if (i.getAnoPublicacao() == null) {
            ps.setNull(6, Types.INTEGER);
        } else {
            ps.setInt(6, i.getAnoPublicacao());
        }
        ps.setString(7, i.getCategoria());
        ps.setString(8, i.getTipo().name());
        ps.setInt(9, i.getQuantidadeTotal());
        ps.setInt(10, i.getQuantidadeDisponivel());
    }

    @Override
    public void excluir(long id) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM item WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao excluir item.", e);
        }
    }

    @Override
    public Optional<Item> buscarPorId(long id) {
        List<Item> l = buscarLista("SELECT " + COLUNAS + " FROM item WHERE id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public Optional<Item> buscarPorIsbn(String isbn) {
        List<Item> l = buscarLista("SELECT " + COLUNAS + " FROM item WHERE isbn = ?", isbn);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public List<Item> pesquisar(String termo) {
        String sql = "SELECT " + COLUNAS + """
                 FROM item
                WHERE LOWER(titulo) LIKE LOWER(?)
                   OR LOWER(autor) LIKE LOWER(?)
                   OR LOWER(categoria) LIKE LOWER(?)
                   OR isbn LIKE ?
                   OR tombo LIKE ?
                ORDER BY titulo
                """;
        String like = "%" + (termo == null ? "" : termo.trim()) + "%";
        return buscarLista(sql, like, like, like, like, like);
    }

    @Override
    public List<Item> listarTodos() {
        return buscarLista("SELECT " + COLUNAS + " FROM item ORDER BY titulo");
    }

    @Override
    public boolean existeIsbn(String isbn, Long ignorarId) {
        if (isbn == null || isbn.isBlank()) {
            return false;
        }
        String sql = "SELECT 1 FROM item WHERE isbn = ?"
                + (ignorarId != null ? " AND id <> ?" : "") + " LIMIT 1";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, isbn);
            if (ignorarId != null) {
                ps.setLong(2, ignorarId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao verificar ISBN duplicado.", e);
        }
    }

    @Override
    public int quantidadeDisponivel(long itemId) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT quantidade_disp FROM item WHERE id = ?")) {
            ps.setLong(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao consultar disponibilidade.", e);
        }
    }

    @Override
    public boolean decrementarDisponivel(long itemId) {
        // A condicao "> 0" evita corrida entre dois atendimentos simultaneos.
        String sql = "UPDATE item SET quantidade_disp = quantidade_disp - 1 "
                + "WHERE id = ? AND quantidade_disp > 0";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, itemId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao baixar a disponibilidade do item.", e);
        }
    }

    @Override
    public void incrementarDisponivel(long itemId) {
        String sql = "UPDATE item SET quantidade_disp = quantidade_disp + 1 "
                + "WHERE id = ? AND quantidade_disp < quantidade_total";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, itemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao repor a disponibilidade do item.", e);
        }
    }

    @Override
    public String gerarProximoTombo() {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(tombo FROM 5) AS INTEGER)), 0) + 1 "
                + "FROM item WHERE tombo LIKE 'ACV-%'";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int proximo = rs.next() ? rs.getInt(1) : 1;
            return String.format("ACV-%05d", proximo);
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao gerar numero de tombo.", e);
        }
    }

    private List<Item> buscarLista(String sql, Object... params) {
        List<Item> lista = new ArrayList<>();
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
            throw new DataAccessException("Falha ao consultar o acervo.", e);
        }
    }

    static Item mapear(ResultSet rs) throws SQLException {
        Item i = new Item();
        i.setId(rs.getLong("id"));
        i.setTombo(rs.getString("tombo"));
        i.setTitulo(rs.getString("titulo"));
        i.setAutor(rs.getString("autor"));
        i.setIsbn(rs.getString("isbn"));
        i.setEditora(rs.getString("editora"));

        int ano = rs.getInt("ano_publicacao");
        i.setAnoPublicacao(rs.wasNull() ? null : ano);

        i.setCategoria(rs.getString("categoria"));
        i.setTipo(TipoItem.valueOf(rs.getString("tipo")));
        i.setQuantidadeTotal(rs.getInt("quantidade_total"));
        i.setQuantidadeDisponivel(rs.getInt("quantidade_disp"));

        if (rs.getTimestamp("criado_em") != null) {
            i.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        }
        return i;
    }
}
