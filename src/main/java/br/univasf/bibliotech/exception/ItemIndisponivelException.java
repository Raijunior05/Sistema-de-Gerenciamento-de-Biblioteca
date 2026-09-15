package br.univasf.bibliotech.exception;

import br.univasf.bibliotech.model.Item;

/** CU 10, fluxo alternativo 3.1 (ponto de extensao "Item indisponivel"). */
public class ItemIndisponivelException extends RegraNegocioException {

    private final transient Item item;

    public ItemIndisponivelException(Item item) {
        super("O item \"" + item.getTitulo() + "\" nao possui exemplares disponiveis. "
                + "Deseja registrar uma reserva?");
        this.item = item;
    }

    public Item getItem() {
        return item;
    }
}
