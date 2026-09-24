package br.univasf.bibliotech.service;

import br.univasf.bibliotech.dao.ItemDAO;
import br.univasf.bibliotech.exception.DadosDuplicadosException;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Caso de Uso 7 - Cadastrar Item. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService - CU 7")
class ItemServiceTest {

    @Mock private ItemDAO itemDAO;

    private ItemService service;

    @BeforeEach
    void preparar() {
        service = new ItemService(itemDAO);
    }

    private Item itemValido() {
        Item i = new Item();
        i.setTitulo("Dom Casmurro");
        i.setAutor("Machado de Assis");
        i.setQuantidadeTotal(3);
        return i;
    }

    @Nested
    @DisplayName("CU 7 - Cadastrar Item")
    class Cadastrar {

        @Test
        @DisplayName("fluxo principal: todos os exemplares entram disponiveis")
        void deveCadastrarComExemplaresDisponiveis() {
            Item item = itemValido();
            item.setIsbn("978-85-359-0277-2");

            service.cadastrar(item);

            assertEquals(3, item.getQuantidadeDisponivel());
            verify(itemDAO).inserir(item);
        }

        @Test
        @DisplayName("ISBN em branco e gravado como nulo para nao colidir no UNIQUE")
        void deveGravarIsbnVazioComoNulo() {
            Item item = itemValido();
            item.setIsbn("   ");

            service.cadastrar(item);

            assertNull(item.getIsbn());
            verify(itemDAO).inserir(item);
        }

        @Test
        @DisplayName("fluxo 5.1: ISBN ja cadastrado")
        void deveRecusarIsbnDuplicado() {
            Item item = itemValido();
            item.setIsbn("978-85-359-0277-2");
            when(itemDAO.existeIsbn("978-85-359-0277-2", null)).thenReturn(true);

            assertThrows(DadosDuplicadosException.class, () -> service.cadastrar(item));

            verify(itemDAO, never()).inserir(any());
        }

        @Test
        @DisplayName("fluxo 5.1: campo obrigatorio vazio")
        void deveExigirTitulo() {
            Item item = itemValido();
            item.setTitulo(" ");

            assertThrows(RegraNegocioException.class, () -> service.cadastrar(item));

            verify(itemDAO, never()).inserir(any());
        }
    }
}
