package br.univasf.bibliotech.exception;

import br.univasf.bibliotech.model.Item;

/** CU 10, fluxo alternativo 3.1 (ponto de extensao "Item indisponivel"). */
public class ItemIndisponivelException extends RegraNegocioException {

    private final transient Item item;

    public ItemIndisponivelException(Item item) {
        this(item, "O item \"" + item.getTitulo() + "\" não possui exemplares disponíveis. "
                + "Deseja registrar uma reserva?");
    }

    public ItemIndisponivelException(Item item, String mensagem) {
        super(mensagem);
        this.item = item;
    }

    public Item getItem() {
        return item;
    }
}
