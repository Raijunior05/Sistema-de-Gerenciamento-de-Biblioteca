package br.univasf.bibliotech.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Pool de conexoes unico da aplicacao (HikariCP).
 *
 * <p>Abrir conexao com o PostgreSQL custa dezenas de milissegundos. O pool
 * mantem conexoes vivas e as reaproveita, de modo que o {@code close()} dos
 * DAOs devolve a conexao em vez de encerra-la. Isso sustenta o RNF-03.</p>
 */
public final class ConnectionFactory {

    private static HikariDataSource dataSource;

    private ConnectionFactory() {
    }

    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(Configuracao.texto("db.url"));
            cfg.setUsername(Configuracao.texto("db.user"));
            cfg.setPassword(Configuracao.texto("db.password"));
            cfg.setMaximumPoolSize(Configuracao.inteiro("db.pool.maximumPoolSize"));
            cfg.setMinimumIdle(Configuracao.inteiro("db.pool.minimumIdle"));
            cfg.setConnectionTimeout(Configuracao.inteiro("db.pool.connectionTimeoutMs"));
            cfg.setPoolName("BiblioTechPool");
            cfg.setAutoCommit(true);
            dataSource = new HikariDataSource(cfg);
        }
        return dataSource;
    }

    /** Encerra o pool. Chamado no stop() da aplicacao JavaFX. */
    public static synchronized void encerrar() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = null;
    }
}
