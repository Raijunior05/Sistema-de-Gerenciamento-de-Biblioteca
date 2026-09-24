package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.exception.RegraNegocioException;
import br.univasf.bibliotech.exception.UsuarioNaoEncontradoException;
import br.univasf.bibliotech.model.Emprestimo;
import br.univasf.bibliotech.model.Item;
import br.univasf.bibliotech.model.Usuario;
import br.univasf.bibliotech.util.Datas;
import br.univasf.bibliotech.util.Sessao;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller dos Casos de Uso 9 (Identificar Usuário) e 10 (Realizar Empréstimo).
 */
public class EmprestimoController {

    @FXML private ComboBox<Item> comboItem;
    @FXML private RadioButton radioCpf;
    @FXML private RadioButton radioMatricula;
    @FXML private TextField campoDocumento;
    @FXML private Label rotuloUsuarioIdentificado;

    @FXML private TextField campoDia;
    @FXML private TextField campoMes;
    @FXML private TextField campoAno;
    @FXML private Label rotuloPrazoDevolucao;

    @FXML private Label mensagemErro;
    @FXML private Button botaoSalvar;

    private Usuario usuarioIdentificado;

    @FXML
    private void initialize() {
        carregarItensAcervo();

        // Data de retirada atual por padrão (CU 10 passo 08)
        LocalDate hoje = LocalDate.now();
        campoDia.setText(String.format("%02d", hoje.getDayOfMonth()));
        campoMes.setText(String.format("%02d", hoje.getMonthValue()));
        campoAno.setText(String.valueOf(hoje.getYear()));

        // Exibe a previsão padrão com base na regra de negócio
        int diasPrazo = App.servicos().emprestimos().getDiasPrazo();
        LocalDate previsao = hoje.plusDays(diasPrazo);
        rotuloPrazoDevolucao.setText("Prazo previsto para devolução: " + Datas.formatar(previsao));
    }

    private void carregarItensAcervo() {
        Task<List<Item>> tarefa = new Task<>() {
            @Override
            protected List<Item> call() {
                return App.servicos().itens().listarTodos();
            }
        };

        tarefa.setOnSucceeded(e -> comboItem.getItems().setAll(tarefa.getValue()));
        tarefa.setOnFailed(e -> Alertas.erro("Erro", "Não foi possível carregar o acervo."));

        Thread t = new Thread(tarefa, "carregar-acervo-emprestimo");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Executa o Caso de Uso 9 - Identificar Usuário.
     */
    @FXML
    private void onIdentificarUsuario() {
        mensagemErro.setText("");
        String documento = campoDocumento.getText() != null ? campoDocumento.getText().trim() : "";

        if (documento.isBlank()) {
            mensagemErro.setText("Digite o CPF ou a Matrícula para identificar o usuário.");
            return;
        }

        try {
            // CU 9 passo 04
            usuarioIdentificado = App.servicos().usuarios().identificar(documento);
            rotuloUsuarioIdentificado.setText("Usuário: " + usuarioIdentificado.getNome());
            rotuloUsuarioIdentificado.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        } catch (UsuarioNaoEncontradoException e) {
            usuarioIdentificado = null;
            rotuloUsuarioIdentificado.setText("Nenhum usuário identificado");
            rotuloUsuarioIdentificado.setStyle("-fx-text-fill: #E53935;");
            mensagemErro.setText(e.getMessage());
        } catch (Exception e) {
            mensagemErro.setText("Erro ao buscar usuário: " + e.getMessage());
        }
    }

    /**
     * Executa o Caso de Uso 10 - Realizar Empréstimo.
     */
    @FXML
    private void onSalvar() {
        mensagemErro.setText("");

        Item itemSelecionado = comboItem.getValue();
        if (itemSelecionado == null) {
            mensagemErro.setText("Selecione um item do acervo para realizar o empréstimo.");
            return;
        }

        if (usuarioIdentificado == null) {
            mensagemErro.setText("Identifique o usuário antes de concluir o empréstimo.");
            return;
        }

        LocalDate dataRetirada = Datas.de(campoDia.getText(), campoMes.getText(), campoAno.getText());
        if (dataRetirada == null) {
            mensagemErro.setText("Data de retirada inválida.");
            return;
        }

        botaoSalvar.setDisable(true);
        Usuario administradorLogado = Sessao.getAdministrador();

        Task<Emprestimo> tarefa = new Task<>() {
            @Override
            protected Emprestimo call() {
                // CU 10 passo 08: Registrar empréstimo no Service
                return App.servicos().emprestimos().registrar(usuarioIdentificado, itemSelecionado, administradorLogado);
            }
        };

        tarefa.setOnSucceeded(e -> {
            botaoSalvar.setDisable(false);
            Emprestimo realizado = tarefa.getValue();

            // Apresenta o código gerado pelo banco e a data prevista
            Alertas.sucesso("Empréstimo Realizado",
                    String.format("Empréstimo registrado com sucesso!\n\nCódigo do Empréstimo: %s\nData Limite para Devolução: %s",
                            realizado.getCodigo(),
                            Datas.formatar(realizado.getDataPrevista())));

            Navegador.irPara(Navegador.Tela.VISAO_GERAL);
        });

        tarefa.setOnFailed(e -> {
            botaoSalvar.setDisable(false);
            Throwable causa = tarefa.getException();

            if (causa instanceof RegraNegocioException) {
                // Trata exceções de limite de empréstimos, itens em atraso ou indisponíveis
                mensagemErro.setText(causa.getMessage());
            } else {
                Alertas.erro("Não foi possível registrar o empréstimo.", causa);
            }
        });

        Thread thread = new Thread(tarefa, "registrar-emprestimo");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onCancelar() {
        Navegador.irPara(Navegador.Tela.VISAO_GERAL);
    }

@FXML
private void onReservarItem() {
    Navegador.irPara(Navegador.Tela.RESERVA);
}
}