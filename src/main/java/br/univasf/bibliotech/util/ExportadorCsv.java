package br.univasf.bibliotech.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Utilitario de exportacao CSV (RFC 4180) com BOM UTF-8 para compatibilidade
 * com o Microsoft Excel.
 *
 * <p>Uso: {@code ExportadorCsv.gravar(caminho, cabecalho, linhas)}
 *
 * <p>Regras de escape aplicadas:
 * <ul>
 *   <li>Se o valor contem {@code ,}, {@code "} ou quebra de linha, e envolto em aspas duplas.</li>
 *   <li>Aspas duplas internas sao dobradas ({@code ""}).</li>
 *   <li>Valores {@code null} sao tratados como string vazia.</li>
 * </ul>
 */
public final class ExportadorCsv {

    /** BOM UTF-8: indica ao Excel que o arquivo e UTF-8. */
    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private ExportadorCsv() {
    }

    /**
     * Grava o arquivo CSV no caminho indicado.
     *
     * @param destino   caminho do arquivo a ser criado/sobrescrito
     * @param cabecalho nomes das colunas (primeira linha)
     * @param linhas    linhas de dados; cada elemento e uma linha, cada {@code String[]} e uma celula
     * @throws IOException erro de I/O ao gravar o arquivo
     */
    public static void gravar(Path destino, String[] cabecalho, List<String[]> linhas)
            throws IOException {
        try (OutputStream os = Files.newOutputStream(destino);
             BufferedWriter bw = new BufferedWriter(
                     new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            os.write(BOM);
            bw.write(formatarLinha(cabecalho));
            bw.newLine();
            for (String[] linha : linhas) {
                bw.write(formatarLinha(linha));
                bw.newLine();
            }
        }
    }

    /**
     * Formata uma linha aplicando escape RFC 4180.
     *
     * @param campos valores da linha
     * @return string CSV da linha (sem quebra de linha no final)
     */
    private static String formatarLinha(String[] campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapar(campos[i]));
        }
        return sb.toString();
    }

    /**
     * Aplica escape RFC 4180 em um unico valor.
     *
     * @param valor valor original; {@code null} e tratado como string vazia
     * @return valor escapado, entre aspas duplas se necessario
     */
    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        boolean precisaAspas = valor.contains(",")
                || valor.contains("\"")
                || valor.contains("\n")
                || valor.contains("\r");
        if (!precisaAspas) {
            return valor;
        }
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }
}
