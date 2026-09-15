package br.univasf.bibliotech.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Item do acervo (Casos de Uso 7 e 8).
 *
 * <h2>Divergencia do prototipo</h2>
 * <p>O prototipo Figma nao possui tela de cadastro de item, e a tela
 * "Pesquisar Acervo" exibe por engano as colunas de emprestimo (leitor,
 * retirada, devolucao). Este model segue o CU 7 passo 03 e o CU 8 passo 03:
 * titulo, autor, ISBN ou identificador, editora, ano, categoria, tipo e
 * quantidade, mais a quantidade disponivel exibida no resultado da busca.</p>
 *
 * <p>O campo {@code tombo} corresponde ao codigo interno gerado pelo sistema
 * no CU 7 passo 07.</p>
 */
public class Item {

    private Long id;
    private String tombo;
    private String titulo;
    private String autor;
    private String isbn;
    private String editora;
    private Integer anoPublicacao;
    private String categoria;
    private TipoItem tipo = TipoItem.LIVRO;
    private int quantidadeTotal;
    private int quantidadeDisponivel;
    private LocalDateTime criadoEm;

    public Item() {
    }

    public Item(Long id, String titulo, int quantidadeDisponivel) {
        this.id = id;
        this.titulo = titulo;
        this.quantidadeDisponivel = quantidadeDisponivel;
    }

    /** CU 10 passo 03: verificacao de disponibilidade. */
    public boolean estaDisponivel() {
        return quantidadeDisponivel > 0;
    }

    /** Quantidade atualmente em maos de usuarios. */
    public int getQuantidadeEmprestada() {
        return quantidadeTotal - quantidadeDisponivel;
    }

    /** Exibicao usada na coluna "Livro / Obra" das tabelas do prototipo. */
    public String getDescricaoCompleta() {
        return titulo + (autor != null ? " - " + autor : "");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTombo() {
        return tombo;
    }

    public void setTombo(String tombo) {
        this.tombo = tombo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getEditora() {
        return editora;
    }

    public void setEditora(String editora) {
        this.editora = editora;
    }

    public Integer getAnoPublicacao() {
        return anoPublicacao;
    }

    public void setAnoPublicacao(Integer anoPublicacao) {
        this.anoPublicacao = anoPublicacao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public TipoItem getTipo() {
        return tipo;
    }

    public void setTipo(TipoItem tipo) {
        this.tipo = tipo;
    }

    public int getQuantidadeTotal() {
        return quantidadeTotal;
    }

    public void setQuantidadeTotal(int quantidadeTotal) {
        this.quantidadeTotal = quantidadeTotal;
    }

    public int getQuantidadeDisponivel() {
        return quantidadeDisponivel;
    }

    public void setQuantidadeDisponivel(int quantidadeDisponivel) {
        this.quantidadeDisponivel = quantidadeDisponivel;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return getDescricaoCompleta();
    }
}
