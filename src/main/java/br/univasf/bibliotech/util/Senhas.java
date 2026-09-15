package br.univasf.bibliotech.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Hash e verificacao de senhas com BCrypt (RNF-05).
 *
 * <p>BCrypt e deliberadamente lento e embute um salt aleatorio em cada hash,
 * o que impede tabelas pre-computadas e torna a forca bruta inviavel. A senha
 * nunca e recuperavel: so e possivel comparar.</p>
 */
public final class Senhas {

    /** Custo 12: cerca de 250 ms por verificacao. */
    private static final int CUSTO = 12;

    private Senhas() {
    }

    /**
     * @param senha senha em texto puro
     * @return hash BCrypt de 60 caracteres
     */
    public static String gerarHash(char[] senha) {
        if (senha == null || senha.length == 0) {
            throw new IllegalArgumentException("A senha nao pode ser vazia.");
        }
        return BCrypt.withDefaults().hashToString(CUSTO, senha);
    }

    public static String gerarHash(String senha) {
        return gerarHash(senha == null ? null : senha.toCharArray());
    }

    /**
     * @param senha senha digitada pelo Administrador
     * @param hash  hash armazenado no banco
     * @return verdadeiro quando conferem
     */
    public static boolean conferem(char[] senha, String hash) {
        if (senha == null || senha.length == 0 || hash == null || hash.isBlank()) {
            return false;
        }
        return BCrypt.verifyer().verify(senha, hash.toCharArray()).verified;
    }

    public static boolean conferem(String senha, String hash) {
        return conferem(senha == null ? null : senha.toCharArray(), hash);
    }
}
