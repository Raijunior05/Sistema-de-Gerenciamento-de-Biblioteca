package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.exception.DadosDuplicadosException;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.model.Item;
import br.univasf.bibliotech.model.TipoItem;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller do Caso de Uso 7 - Cadastrar Item.
 */
public class CadastroItemController {

    @FXML private TextField campoTitulo;
    @FXML private TextField campoAutor;
    @FXML private TextField campoIsbn;
    @FXML private TextField campoEditora;
    @FXML private TextField campoAno;
    @FXML private TextField campoQuantidade;
    @FXML private ComboBox<TipoItem> comboTipo;
    @FXML private TextField campoCategoria;

    @FXML private Label mensagemErro;
    @FXML private Button botaoSalvar;

    @FXML
    private void initialize() {
        comboTipo.getItems().setAll(TipoItem.values());
        comboTipo.setValue(TipoItem.LIVRO);
    }

    @FXML
    private void onSalvar() {
        mensagemErro.setText("");

        Item item = new Item();
        item.setTitulo(campoTitulo.getText() != null ? campoTitulo.getText().trim() : "");
        item.setAutor(campoAutor.getText() != null ? campoAutor.getText().trim() : "");
        item.setIsbn(campoIsbn.getText() != null ? campoIsbn.getText().trim() : "");
        item.setEditora(campoEditora.getText() != null ? campoEditora.getText().trim() : "");
        item.setCategoria(campoCategoria.getText() != null ? campoCategoria.getText().trim() : "");
        item.setTipo(comboTipo.getValue());

        String textoAno = campoAno.getText() != null ? campoAno.getText().trim() : "";
        if (!textoAno.isBlank()) {
            try {
                item.setAnoPublicacao(Integer.parseInt(textoAno));
            } catch (NumberFormatException e) {
                mensagemErro.setText("Ano de publicação inválido.");
                return;
            }
        }

        String textoQtd = campoQuantidade.getText() != null ? campoQuantidade.getText().trim() : "";
        if (textoQtd.isBlank()) {
            mensagemErro.setText("Informe a quantidade de exemplares.");
            return;
        }

        try {
            int qtd = Integer.parseInt(textoQtd);
            item.setQuantidadeTotal(qtd);
        } catch (NumberFormatException e) {
            mensagemErro.setText("A quantidade deve ser um número inteiro.");
            return;
        }

        botaoSalvar.setDisable(true);

        Task<Item> tarefa = new Task<>() {
            @Override
            protected Item call() {
                App.servicos().itens().cadastrar(item);
                return item;
            }
        };

        tarefa.setOnSucceeded(e -> {
            botaoSalvar.setDisable(false);
            Item cadastrado = tarefa.getValue();

            // CU 7 passo 08: mensagem de sucesso com os dados do item cadastrado
            Alertas.sucesso("Item cadastrado", "Item cadastrado com sucesso no acervo!\n\n"
                    + "Nº de tombo: " + cadastrado.getTombo() + "\n"
                    + "Título: " + cadastrado.getTitulo() + "\n"
                    + "Autor: " + cadastrado.getAutor() + "\n"
                    + "ISBN/identificador: " + informado(cadastrado.getIsbn()) + "\n"
                    + "Editora: " + informado(cadastrado.getEditora()) + "\n"
                    + "Ano: " + (cadastrado.getAnoPublicacao() == null
                            ? "Não informado" : cadastrado.getAnoPublicacao()) + "\n"
                    + "Categoria: " + informado(cadastrado.getCategoria()) + "\n"
                    + "Tipo: " + cadastrado.getTipo() + "\n"
                    + "Quantidade: " + cadastrado.getQuantidadeTotal());

            Navegador.irPara(Navegador.Tela.VISAO_GERAL);
        });

        tarefa.setOnFailed(e -> {
            botaoSalvar.setDisable(false);
            Throwable causa = tarefa.getException();

            if (causa instanceof DadosDuplicadosException || causa instanceof RegraNegocioException) {
                // Trata erro de ISBN duplicado e regras de negócio diretamente na mensagem inline
                mensagemErro.setText(causa.getMessage());
            } else {
                Alertas.erro("Não foi possível cadastrar o item.", causa);
            }
        });

        Thread thread = new Thread(tarefa, "cadastrar-item");
        thread.setDaemon(true);
        thread.start();
    }

    private static String informado(String valor) {
        return valor == null || valor.isBlank() ? "Não informado" : valor;
    }

    @FXML
    private void onCancelar() {
        Navegador.irPara(Navegador.Tela.VISAO_GERAL);
    }
}