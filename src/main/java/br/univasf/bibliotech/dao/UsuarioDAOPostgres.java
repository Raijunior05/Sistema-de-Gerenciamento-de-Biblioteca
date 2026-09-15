package br.univasf.bibliotech.dao;

import br.univasf.bibliotech.exception.DataAccessException;
import br.univasf.bibliotech.model.Perfil;
import br.univasf.bibliotech.model.Usuario;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDAOPostgres implements UsuarioDAO {

    private static final String COLUNAS = """
            id, nome, email, cpf, matricula, telefone, nascimento,
            perfil, login, senha_hash, ativo, criado_em
            """;

    private final DataSource dataSource;

    public UsuarioDAOPostgres(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void inserir(Usuario u) {
        String sql = """
                INSERT INTO usuario
                    (nome, email, cpf, matricula, telefone, nascimento, perfil, login, senha_hash, ativo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preencher(ps, u);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    u.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao inserir usuario.", e);
        }
    }

    @Override
    public void atualizar(Usuario u) {
        String sql = """
                UPDATE usuario SET
                    nome = ?, email = ?, cpf = ?, matricula = ?, telefone = ?,
                    nascimento = ?, perfil = ?, login = ?, senha_hash = ?, ativo = ?
                WHERE id = ?
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            preencher(ps, u);
            ps.setLong(11, u.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Falha ao atualizar usuario.", e);
        }
    }

    private void preencher(PreparedStatement ps, Usuario u) throws SQLException {
        ps.setString(1, u.getNome());
        ps.setString(2, u.getEmail());
        ps.setString(3, u.getCpf());
        ps.setString(4, u.getMatricula());
        ps.setString(5, u.getTelefone());
        ps.setDate(6, u.getNascimento() == null ? null : Date.valueOf(u.getNascimento()));
        ps.setString(7, u.getPerfil().name());
        ps.setString(8, u.getLogin());
        ps.setString(9, u.getSenhaHash());
        ps.setBoolean(10, u.isAtivo());
    }

    @Override
    public void excluir(long id) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM usuario WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao excluir usuario.", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(long id) {
        return buscarUm("SELECT " + COLUNAS + " FROM usuario WHERE id = ?", id);
    }

    @Override
    public Optional<Usuario> buscarPorIdentificacao(String cpfOuMatricula) {
        String sql = "SELECT " + COLUNAS + " FROM usuario WHERE cpf = ? OR matricula = ?";
        return buscarUm(sql, cpfOuMatricula, cpfOuMatricula);
    }

    @Override
    public Optional<Usuario> buscarPorCredencial(String emailOuLogin) {
        String sql = "SELECT " + COLUNAS + """
                 FROM usuario
                WHERE (LOWER(email) = LOWER(?) OR LOWER(login) = LOWER(?))
                  AND perfil = 'ADMINISTRADOR'
                  AND ativo = TRUE
                """;
        return buscarUm(sql, emailOuLogin, emailOuLogin);
    }

    @Override
    public List<Usuario> pesquisar(String termo) {
        String sql = "SELECT " + COLUNAS + """
                 FROM usuario
                WHERE LOWER(nome) LIKE LOWER(?)
                   OR LOWER(email) LIKE LOWER(?)
                   OR cpf LIKE ?
                   OR matricula LIKE ?
                ORDER BY nome
                """;
        String like = "%" + (termo == null ? "" : termo.trim()) + "%";
        return buscarLista(sql, like, like, like, like);
    }

    @Override
    public List<Usuario> listarTodos() {
        return buscarLista("SELECT " + COLUNAS + " FROM usuario ORDER BY nome");
    }

    @Override
    public boolean existeEmail(String email, Long ignorarId) {
        return existe("LOWER(email) = LOWER(?)", email, ignorarId);
    }

    @Override
    public boolean existeCpf(String cpf, Long ignorarId) {
        return cpf != null && existe("cpf = ?", cpf, ignorarId);
    }

    @Override
    public boolean existeMatricula(String matricula, Long ignorarId) {
        return matricula != null && existe("matricula = ?", matricula, ignorarId);
    }

    @Override
    public boolean existeLogin(String login, Long ignorarId) {
        return login != null && existe("LOWER(login) = LOWER(?)", login, ignorarId);
    }

    private boolean existe(String condicao, String valor, Long ignorarId) {
        String sql = "SELECT 1 FROM usuario WHERE " + condicao
                + (ignorarId != null ? " AND id <> ?" : "") + " LIMIT 1";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, valor);
            if (ignorarId != null) {
                ps.setLong(2, ignorarId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao verificar duplicidade.", e);
        }
    }

    @Override
    public boolean possuiEmprestimoOuReservaAtiva(long id) {
        String sql = """
                SELECT 1 FROM emprestimo
                 WHERE usuario_id = ? AND status <> 'CONCLUIDO'
                UNION ALL
                SELECT 1 FROM reserva
                 WHERE usuario_id = ? AND status IN ('AGUARDANDO', 'DISPONIVEL')
                LIMIT 1
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao verificar pendencias do usuario.", e);
        }
    }

    @Override
    public long contarAdministradores() {
        String sql = "SELECT COUNT(*) FROM usuario WHERE perfil = 'ADMINISTRADOR' AND ativo = TRUE";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao contar administradores.", e);
        }
    }

    @Override
    public int contarCadastradosDesde(LocalDate inicio) {
        String sql = "SELECT COUNT(*) FROM usuario WHERE criado_em >= ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(inicio));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao contar cadastros.", e);
        }
    }

    private Optional<Usuario> buscarUm(String sql, Object... params) {
        List<Usuario> lista = buscarLista(sql, params);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    private List<Usuario> buscarLista(String sql, Object... params) {
        List<Usuario> lista = new ArrayList<>();
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
            throw new DataAccessException("Falha ao consultar usuarios.", e);
        }
    }

    static Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getLong("id"));
        u.setNome(rs.getString("nome"));
        u.setEmail(rs.getString("email"));
        u.setCpf(rs.getString("cpf"));
        u.setMatricula(rs.getString("matricula"));
        u.setTelefone(rs.getString("telefone"));

        Date nasc = rs.getDate("nascimento");
        u.setNascimento(nasc == null ? null : nasc.toLocalDate());

        u.setPerfil(Perfil.valueOf(rs.getString("perfil")));
        u.setLogin(rs.getString("login"));
        u.setSenhaHash(rs.getString("senha_hash"));
        u.setAtivo(rs.getBoolean("ativo"));

        if (rs.getTimestamp("criado_em") != null) {
            u.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
        }
        return u;
    }
}
