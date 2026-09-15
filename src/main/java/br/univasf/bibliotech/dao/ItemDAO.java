package br.univasf.bibliotech.dao;

import br.univasf.bibliotech.model.Item;

import java.util.List;
import java.util.Optional;

/** Acesso a dados do acervo (Casos de Uso 7 e 8). */
public interface ItemDAO {

    /** CU 7 passo 06. Preenche id e tombo gerados. */
    void inserir(Item item);

    void atualizar(Item item);

    void excluir(long id);

    Optional<Item> buscarPorId(long id);

    Optional<Item> buscarPorIsbn(String isbn);

    /** CU 8 passo 02: titulo, autor, categoria, ISBN ou tombo. */
    List<Item> pesquisar(String termo);

    List<Item> listarTodos();

    boolean existeIsbn(String isbn, Long ignorarId);

    /** CU 10 passo 03. */
    int quantidadeDisponivel(long itemId);

    /** CU 10 passo 09: decrementa em 1. Retorna falso se ja estava zerado. */
    boolean decrementarDisponivel(long itemId);

    /** CU 12 passo 07: incrementa em 1. */
    void incrementarDisponivel(long itemId);

    /** CU 7 passo 07: proximo numero de tombo. */
    String gerarProximoTombo();
}
