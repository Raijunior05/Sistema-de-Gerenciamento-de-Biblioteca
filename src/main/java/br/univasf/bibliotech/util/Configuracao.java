package br.univasf.bibliotech.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Leitura centralizada de {@code database.properties}.
 *
 * <p>Procura primeiro um arquivo {@code database.properties} ao lado do
 * executavel; se nao existir, cai para o arquivo empacotado em resources.
 * Isso permite que a mesma build rode com o banco em Docker ou com uma
 * instalacao nativa do PostgreSQL, sem recompilar.</p>
 */
public final class Configuracao {

    private static final String ARQUIVO = "database.properties";
    private static final Properties PROPS = carregar();

    private Configuracao() {
    }

    private static Properties carregar() {
        Properties p = new Properties();

        Path externo = Path.of(ARQUIVO);
        if (Files.exists(externo)) {
            try (InputStream in = Files.newInputStream(externo)) {
                p.load(in);
                return p;
            } catch (IOException e) {
                throw new IllegalStateException("Falha ao ler " + externo.toAbsolutePath(), e);
            }
        }

        try (InputStream in = Configuracao.class.getClassLoader().getResourceAsStream(ARQUIVO)) {
            if (in == null) {
                throw new IllegalStateException(ARQUIVO + " nao encontrado no classpath.");
            }
            p.load(in);
            return p;
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler " + ARQUIVO + " do classpath", e);
        }
    }

    public static String texto(String chave) {
        String valor = PROPS.getProperty(chave);
        if (valor == null) {
            throw new IllegalStateException("Propriedade ausente: " + chave);
        }
        return valor.trim();
    }

    public static int inteiro(String chave) {
        return Integer.parseInt(texto(chave));
    }

}
