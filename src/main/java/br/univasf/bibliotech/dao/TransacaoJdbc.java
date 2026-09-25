package br.univasf.bibliotech.dao;

import br.univasf.bibliotech.exception.DataAccessException;
import br.univasf.bibliotech.util.Transacao;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Transacao JDBC compartilhada pelos DAOs (CU 10 passos 08 e 09, CU 12 passos 06 e 07).
 *
 * <p>Tambem e o DataSource entregue aos DAOs: dentro de {@link #executar}, todo
 * {@code getConnection()} da mesma thread recebe a conexao da transacao, e o
 * {@code close()} do try-with-resources do DAO nao a devolve ao pool antes do
 * commit ou rollback. Fora de uma transacao, delega ao pool normalmente.</p>
 */
public class TransacaoJdbc implements Transacao, DataSource {

    private final DataSource pool;
    private final ThreadLocal<Connection> conexaoAtual = new ThreadLocal<>();

    public TransacaoJdbc(DataSource pool) {
        this.pool = pool;
    }

    @Override
    public <T> T executar(Supplier<T> acao) {
        if (conexaoAtual.get() != null) {
            return acao.get();
        }
        try (Connection conexao = pool.getConnection()) {
            conexao.setAutoCommit(false);
            conexaoAtual.set(conexao);
            try {
                T resultado = acao.get();
                conexao.commit();
                return resultado;
            } catch (RuntimeException | Error e) {
                conexao.rollback();
                throw e;
            } finally {
                conexaoAtual.remove();
                conexao.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao controlar a transacao.", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conexao = conexaoAtual.get();
        return conexao == null ? pool.getConnection() : semFechamento(conexao);
    }

    @Override
    public Connection getConnection(String usuario, String senha) throws SQLException {
        return pool.getConnection(usuario, senha);
    }

    private static Connection semFechamento(Connection conexao) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (proxy, metodo, argumentos) -> {
                    if ("close".equals(metodo.getName())) {
                        return null;
                    }
                    try {
                        return metodo.invoke(conexao, argumentos);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return pool.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter saida) throws SQLException {
        pool.setLogWriter(saida);
    }

    @Override
    public void setLoginTimeout(int segundos) throws SQLException {
        pool.setLoginTimeout(segundos);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return pool.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return pool.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> tipo) throws SQLException {
        return tipo.isInstance(this) ? tipo.cast(this) : pool.unwrap(tipo);
    }

    @Override
    public boolean isWrapperFor(Class<?> tipo) throws SQLException {
        return tipo.isInstance(this) || pool.isWrapperFor(tipo);
    }
}
