package br.univasf.bibliotech.util;

import br.univasf.bibliotech.model.Usuario;

/**
 * Administrador autenticado na sessao corrente.
 *
 * <p>Diferenca em relacao a uma aplicacao web: nao ha cookie nem token. Como
 * a aplicacao e um processo unico, o usuario logado permanece em memoria do
 * login ate o encerramento.</p>
 */
public final class Sessao {

    private static Usuario administrador;

    private Sessao() {
    }

    public static void iniciar(Usuario usuario) {
        administrador = usuario;
    }

    public static void encerrar() {
        administrador = null;
    }

    public static boolean estaAutenticado() {
        return administrador != null;
    }

    /** @throws IllegalStateException se nao houver sessao ativa */
    public static Usuario getAdministrador() {
        if (administrador == null) {
            throw new IllegalStateException("Nenhum administrador autenticado.");
        }
        return administrador;
    }

    /** Saudacao do cabecalho ("Ola, {nome}"). */
    public static String getPrimeiroNome() {
        if (administrador == null || administrador.getNome() == null) {
            return "";
        }
        return administrador.getNome().split("\\s+")[0];
    }
}
