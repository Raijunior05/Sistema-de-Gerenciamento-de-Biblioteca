package br.univasf.bibliotech.service;

import br.univasf.bibliotech.dao.ItemDAO;
import br.univasf.bibliotech.exception.DadosDuplicadosException;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Item;

import java.time.Year;
import java.util.List;

/** Casos de Uso 7 (Cadastrar Item) e 8 (Pesquisar Acervo). */
public class ItemService {

    private final ItemDAO itemDAO;

    public ItemService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    /**
     * CU 7 - Cadastrar Item.
     *
     * <p>O tombo (passo 07) e gerado pelo DAO, nao informado pelo
     * Administrador.</p>
     *
     * @throws DadosDuplicadosException fluxo alternativo 5.1
     */
    public void cadastrar(Item item) {
        validar(item, null);

        if (itemDAO.existeIsbn(item.getIsbn(), null)) {
            throw new DadosDuplicadosException("ISBN", item.getIsbn());
        }
        // Um item recem-cadastrado tem todos os exemplares disponiveis.
        item.setQuantidadeDisponivel(item.getQuantidadeTotal());
        itemDAO.inserir(item);
    }

    public void editar(Item item) {
        if (item.getId() == null) {
            throw new RegraNegocioException("Item sem identificador nao pode ser editado.");
        }
        validar(item, item.getId());

        if (itemDAO.existeIsbn(item.getIsbn(), item.getId())) {
            throw new DadosDuplicadosException("ISBN", item.getIsbn());
        }
        itemDAO.atualizar(item);
    }

    /** CU 8 - Pesquisar Acervo. */
    public List<Item> pesquisar(String termo) {
        if (termo == null || termo.isBlank()) {
            return itemDAO.listarTodos();
        }
        return itemDAO.pesquisar(termo);
    }

    public List<Item> listarTodos() {
        return itemDAO.listarTodos();
    }

    public Item buscar(long id) {
        return itemDAO.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Item nao encontrado no acervo."));
    }

    private void validar(Item i, Long ignorarId) {
        if (i.getTitulo() == null || i.getTitulo().isBlank()) {
            throw new RegraNegocioException("Informe o titulo do item.");
        }
        if (i.getAutor() == null || i.getAutor().isBlank()) {
            throw new RegraNegocioException("Informe o autor do item.");
        }
        if (i.getQuantidadeTotal() <= 0) {
            throw new RegraNegocioException("A quantidade deve ser maior que zero.");
        }
        int anoAtual = Year.now().getValue();
        if (i.getAnoPublicacao() != null
                && (i.getAnoPublicacao() < 1400 || i.getAnoPublicacao() > anoAtual + 1)) {
            throw new RegraNegocioException("Ano de publicacao invalido.");
        }
        if (ignorarId != null && i.getQuantidadeDisponivel() > i.getQuantidadeTotal()) {
            throw new RegraNegocioException(
                    "A quantidade disponivel nao pode exceder a quantidade total.");
        }
    }
}
