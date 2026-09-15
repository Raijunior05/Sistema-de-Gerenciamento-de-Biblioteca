package br.univasf.bibliotech.model;

/** Tipos de item do acervo (CU 7 passo 03). */
public enum TipoItem {

    LIVRO("Livro"),
    REVISTA("Revista"),
    PERIODICO("Periodico"),
    MIDIA("Midia"),
    OUTRO("Outro");

    private final String descricao;

    TipoItem(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
