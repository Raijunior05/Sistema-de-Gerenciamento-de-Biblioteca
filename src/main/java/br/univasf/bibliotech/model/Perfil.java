package br.univasf.bibliotech.model;

/**
 * Perfis de acesso do sistema.
 *
 * <p>O ator Bibliotecario foi removido na versao 3 do documento de requisitos:
 * suas funcoes foram absorvidas pelo Administrador. O antigo ator Leitor
 * passou a se chamar Usuario.</p>
 */
public enum Perfil {

    /** Unico perfil que faz login na aplicacao (CU 1). */
    ADMINISTRADOR("Administrador"),

    /** Cliente da biblioteca. Possui cadastro, mas nao acessa o sistema. */
    USUARIO("Usuario");

    private final String descricao;

    Perfil(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean exigeCredenciais() {
        return this == ADMINISTRADOR;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
