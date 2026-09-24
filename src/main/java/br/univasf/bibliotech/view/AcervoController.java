package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.model.Item;
import br.univasf.bibliotech.util.Sessao;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * Controller do Caso de Uso 8 - Pesquisar Acervo.
 *
 * <p>Segue o mesmo padrão visual e de consulta da tela
 * "Consultar Usuário": busca assíncrona, tabela inicialmente oculta
 * e ação para visualizar os dados completos do item.</p>
 */
public class AcervoController {

    @FXML private TextField campoBusca;
    @FXML private TableView<Item> tabelaAcervo;

    @FXML private TableColumn<Item, String> colunaTombo;
    @FXML private TableColumn<Item, String> colunaTitulo;
    @FXML private TableColumn<Item, String> colunaAutor;
    @FXML private TableColumn<Item, String> colunaTipo;
    @FXML private TableColumn<Item, String> colunaCategoria;
    @FXML private TableColumn<Item, String> colunaDisponibilidade;
    @FXML private TableColumn<Item, String> colunaAcoes;

    @FXML private javafx.scene.control.Label rodapeNome;

    @FXML
    private void initialize() {
        if (Sessao.estaAutenticado()) {
            rodapeNome.setText(Sessao.getAdministrador().getNome());
        }

        configurarTabela();

        tabelaAcervo.setVisible(false);
        tabelaAcervo.setManaged(false);
    }

    private void configurarTabela() {
        colunaTombo.setCellValueFactory(dados ->
                texto(dados.getValue().getTombo()));

        colunaTitulo.setCellValueFactory(dados ->
                texto(dados.getValue().getTitulo()));

        colunaAutor.setCellValueFactory(dados ->
                texto(dados.getValue().getAutor()));

        colunaTipo.setCellValueFactory(dados ->
                texto(dados.getValue().getTipo() != null
                        ? dados.getValue().getTipo().toString()
                        : ""));

        colunaCategoria.setCellValueFactory(dados ->
                texto(dados.getValue().getCategoria()));

        colunaDisponibilidade.setCellValueFactory(dados -> {
            Item item = dados.getValue();
            return texto(item.getQuantidadeDisponivel()
                    + " / " + item.getQuantidadeTotal());
        });

        colunaAcoes.setCellValueFactory(dados ->
                new SimpleStringProperty("acao"));

        colunaAcoes.setCellFactory(coluna -> new TableCell<>() {
            private final Button botao = new Button("◉");

            {
                botao.getStyleClass().add("botao-acao-consulta");
                botao.setFocusTraversable(false);
                botao.setOnAction(evento -> {
                    int indice = getIndex();

                    if (indice < 0 || indice >= getTableView().getItems().size()) {
                        return;
                    }

                    mostrarItem(getTableView().getItems().get(indice));
                });
            }

            @Override
            protected void updateItem(String item, boolean vazio) {
                super.updateItem(item, vazio);

                if (vazio) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(botao);
                    setText(null);
                }
            }
        });
    }

    private SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor != null ? valor : "");
    }

    /**
     * CU 8 - pesquisa por título, autor, categoria, ISBN ou tombo.
     * Quando o campo está vazio, o acervo completo é listado.
     */
    @FXML
    private void onPesquisar() {
        String termo = campoBusca.getText() != null
                ? campoBusca.getText().trim()
                : "";

        tabelaAcervo.setVisible(true);
        tabelaAcervo.setManaged(true);
        tabelaAcervo.setPlaceholder(new javafx.scene.control.Label("Consultando acervo..."));

        Task<List<Item>> tarefa = new Task<>() {
            @Override
            protected List<Item> call() {
                return App.servicos().itens().pesquisar(termo);
            }
        };

        tarefa.setOnSucceeded(evento -> {
            List<Item> itens = tarefa.getValue();

            tabelaAcervo.getItems().setAll(itens);

            if (itens.isEmpty()) {
                tabelaAcervo.setPlaceholder(
                        new javafx.scene.control.Label("Nenhum item encontrado.")
                );
            }
        });

        tarefa.setOnFailed(evento -> {
            tabelaAcervo.getItems().clear();
            tabelaAcervo.setPlaceholder(
                    new javafx.scene.control.Label("Não foi possível consultar o acervo.")
            );

            Alertas.erro(
                    "Não foi possível consultar o acervo.",
                    tarefa.getException()
            );
        });

        Thread thread = new Thread(tarefa, "pesquisar-acervo");
        thread.setDaemon(true);
        thread.start();
    }

    private void mostrarItem(Item item) {
        String isbn = item.getIsbn() != null && !item.getIsbn().isBlank()
                ? item.getIsbn()
                : "Não informado";

        String editora = item.getEditora() != null && !item.getEditora().isBlank()
                ? item.getEditora()
                : "Não informada";

        String ano = item.getAnoPublicacao() != null
                ? String.valueOf(item.getAnoPublicacao())
                : "Não informado";

        String categoria = item.getCategoria() != null && !item.getCategoria().isBlank()
                ? item.getCategoria()
                : "Não informada";

        String tipo = item.getTipo() != null
                ? item.getTipo().toString()
                : "Não informado";

        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Detalhes do item");
        alerta.setHeaderText(item.getTitulo());
        alerta.setContentText(
                "Tombo: " + valor(item.getTombo()) + "\n"
                        + "Autor: " + valor(item.getAutor()) + "\n"
                        + "ISBN: " + isbn + "\n"
                        + "Editora: " + editora + "\n"
                        + "Ano de publicação: " + ano + "\n"
                        + "Categoria: " + categoria + "\n"
                        + "Tipo: " + tipo + "\n"
                        + "Disponibilidade: "
                        + item.getQuantidadeDisponivel()
                        + " de "
                        + item.getQuantidadeTotal()
        );
        alerta.showAndWait();
    }

    private String valor(String valor) {
        return valor != null && !valor.isBlank() ? valor : "Não informado";
    }

    @FXML
    private void onVoltar() {
        Navegador.irPara(Navegador.Tela.VISAO_GERAL);
    }

    @FXML
    private void onSair() {
        if (Alertas.confirmar("Encerrar sessão", "Deseja sair do sistema?")) {
            Sessao.encerrar();
            Navegador.irPara(Navegador.Tela.LOGIN);
        }
    }
}
