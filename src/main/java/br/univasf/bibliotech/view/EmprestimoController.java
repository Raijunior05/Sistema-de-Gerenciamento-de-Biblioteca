package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.exception.ItemIndisponivelException;
import br.univasf.bibliotech.exception.RegraNegocioException;
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
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller dos Casos de Uso 9 (Identificar Usuário) e 10 (Realizar Empréstimo).
 *
 * <p>Segue a ordem do diagrama de sequência: item (passo 02), disponibilidade
 * (passo 03), identificação (passos 04 a 06) e registro (passos 07 a 10).</p>
 */
public class EmprestimoController {

    private record Disponibilidade(int noAcervo, int livres) {
    }

    @FXML private ComboBox<Item> comboItem;
    @FXML private Label rotuloDisponibilidade;
    @FXML private TextField campoDocumento;
    @FXML private Button botaoIdentificar;
    @FXML private Label rotuloUsuarioIdentificado;

    @FXML private TextField campoDia;
    @FXML private TextField campoMes;
    @FXML private TextField campoAno;
    @FXML private Label rotuloPrazoDevolucao;

    @FXML private Label mensagemErro;
    @FXML private Button botaoReservar;
    @FXML private Button botaoSalvar;

    private Item itemDisponivel;
    private Usuario usuarioIdentificado;

    @FXML
    private void initialize() {
        carregarItensAcervo();

        // CU 10 passo 08: a data do empréstimo é a data atual, registrada pelo sistema
        LocalDate hoje = LocalDate.now();
        campoDia.setText(String.format("%02d", hoje.getDayOfMonth()));
        campoMes.setText(String.format("%02d", hoje.getMonthValue()));
        campoAno.setText(String.valueOf(hoje.getYear()));

        int diasPrazo = App.servicos().emprestimos().getDiasPrazo();
        rotuloPrazoDevolucao.setText("Prazo previsto para devolução: "
                + Datas.formatar(hoje.plusDays(diasPrazo)));

        campoDocumento.textProperty().addListener((obs, anterior, atual) -> limparUsuario());
    }

    private void carregarItensAcervo() {
        Task<List<Item>> tarefa = new Task<>() {
            @Override
            protected List<Item> call() {
                return App.servicos().itens().listarTodos();
            }
        };

        tarefa.setOnSucceeded(e -> comboItem.getItems().setAll(tarefa.getValue()));
        tarefa.setOnFailed(e -> Alertas.erro("Não foi possível carregar o acervo.",
                tarefa.getException()));

        iniciar(tarefa, "carregar-acervo-emprestimo");
    }

    /** CU 10 passo 03 - verificarDisponibilidade(id_item). */
    @FXML
    private void onVerificarDisponibilidade() {
        Item selecionado = comboItem.getValue();
        itemDisponivel = null;
        mensagemErro.setText("");
        mostrarOfertaReserva(false);
        liberarIdentificacao(false);

        if (selecionado == null) {
            rotuloDisponibilidade.setText("Selecione o item para verificar a disponibilidade.");
            return;
        }

        rotuloDisponibilidade.setText("Verificando disponibilidade...");
        comboItem.setDisable(true);

        Task<Disponibilidade> tarefa = new Task<>() {
            @Override
            protected Disponibilidade call() {
                long id = selecionado.getId();
                return new Disponibilidade(
                        App.servicos().itens().buscar(id).getQuantidadeDisponivel(),
                        App.servicos().emprestimos().verificarDisponibilidade(id));
            }
        };

        tarefa.setOnSucceeded(e -> {
            comboItem.setDisable(false);
            if (comboItem.getValue() != selecionado) {
                return;
            }
            Disponibilidade d = tarefa.getValue();
            if (d.noAcervo() <= 0) {
                // Fluxo 3.1: informa a indisponibilidade e oferece a reserva
                rotuloDisponibilidade.setText("Item indisponível: nenhum exemplar no acervo. "
                        + "O empréstimo é encerrado; ofereça a reserva ao Usuário.");
                mostrarOfertaReserva(true);
                return;
            }
            itemDisponivel = selecionado;
            rotuloDisponibilidade.setText(d.livres() > 0
                    ? "Disponível: " + d.livres() + " exemplar(es). Identifique o Usuário."
                    : "Exemplar separado para o 1º da fila de reserva: somente esse Usuário "
                            + "pode retirá-lo.");
            liberarIdentificacao(true);
            campoDocumento.requestFocus();
        });

        tarefa.setOnFailed(e -> {
            comboItem.setDisable(false);
            rotuloDisponibilidade.setText("Não foi possível verificar a disponibilidade.");
            Alertas.erro("Não foi possível verificar a disponibilidade.", tarefa.getException());
        });

        iniciar(tarefa, "verificar-disponibilidade");
    }

    /** CU 10 passos 04 a 06 - inclui o CU 9 (Identificar Usuário). */
    @FXML
    private void onIdentificarUsuario() {
        if (itemDisponivel == null) {
            return;
        }
        mensagemErro.setText("");
        String documento = campoDocumento.getText() != null ? campoDocumento.getText().trim() : "";

        if (documento.isBlank()) {
            mensagemErro.setText("Digite o CPF ou a matrícula para identificar o usuário.");
            return;
        }

        botaoIdentificar.setDisable(true);

        Task<Usuario> tarefa = new Task<>() {
            @Override
            protected Usuario call() {
                return App.servicos().usuarios().identificar(documento);
            }
        };

        tarefa.setOnSucceeded(e -> {
            botaoIdentificar.setDisable(false);
            usuarioIdentificado = tarefa.getValue();
            // CU 9 passo 05: exibe os dados do Usuário encontrado
            rotuloUsuarioIdentificado.setText(DadosUsuario.formatar(usuarioIdentificado));
            marcarIdentificacao(true);
            botaoSalvar.setDisable(false);
        });

        tarefa.setOnFailed(e -> {
            botaoIdentificar.setDisable(false);
            limparUsuario();
            marcarIdentificacao(false);
            Throwable causa = tarefa.getException();
            if (causa instanceof RegraNegocioException) {
                // Fluxo 6.1 (CU 9, fluxo 4.1): mensagem e retorno ao passo 04
                mensagemErro.setText(causa.getMessage());
            } else {
                Alertas.erro("Não foi possível identificar o usuário.", causa);
            }
        });

        iniciar(tarefa, "identificar-usuario-emprestimo");
    }

    /** CU 10 passos 07 a 10. */
    @FXML
    private void onSalvar() {
        mensagemErro.setText("");

        if (itemDisponivel == null) {
            mensagemErro.setText("Selecione um item disponível do acervo.");
            return;
        }
        if (usuarioIdentificado == null) {
            mensagemErro.setText("Identifique o usuário antes de concluir o empréstimo.");
            return;
        }

        botaoSalvar.setDisable(true);
        Item item = itemDisponivel;
        Usuario usuario = usuarioIdentificado;
        Usuario administradorLogado = Sessao.getAdministrador();

        Task<Emprestimo> tarefa = new Task<>() {
            @Override
            protected Emprestimo call() {
                return App.servicos().emprestimos().registrar(usuario, item, administradorLogado);
            }
        };

        tarefa.setOnSucceeded(e -> {
            Emprestimo realizado = tarefa.getValue();

            // Passo 10: confirmação com a data prevista de devolução
            Alertas.sucesso("Empréstimo realizado",
                    String.format("Empréstimo registrado com sucesso!%n%n"
                                    + "Código do empréstimo: %s%nUsuário: %s%nItem: %s%n"
                                    + "Data prevista para devolução: %s",
                            realizado.getCodigo(), usuario.getNome(), item.getTitulo(),
                            Datas.formatar(realizado.getDataPrevista())));

            Navegador.irPara(Navegador.Tela.VISAO_GERAL);
        });

        tarefa.setOnFailed(e -> {
            botaoSalvar.setDisable(false);
            Throwable causa = tarefa.getException();

            if (causa instanceof ItemIndisponivelException) {
                // Fluxo 3.1: a disponibilidade mudou ou o exemplar está separado para reserva
                mensagemErro.setText(causa.getMessage());
                mostrarOfertaReserva(true);
            } else if (causa instanceof RegraNegocioException) {
                // Fluxo 7.1: limite de empréstimos ou pendência
                mensagemErro.setText(causa.getMessage());
            } else {
                Alertas.erro("Não foi possível registrar o empréstimo.", causa);
            }
        });

        iniciar(tarefa, "registrar-emprestimo");
    }

    private void liberarIdentificacao(boolean liberar) {
        campoDocumento.setDisable(!liberar);
        botaoIdentificar.setDisable(!liberar);
        if (!liberar) {
            limparUsuario();
        }
    }

    private void limparUsuario() {
        usuarioIdentificado = null;
        rotuloUsuarioIdentificado.setText("Nenhum usuário identificado");
        rotuloUsuarioIdentificado.getStyleClass()
                .removeAll("dados-usuario-encontrado", "dados-usuario-ausente");
        botaoSalvar.setDisable(true);
    }

    private void marcarIdentificacao(boolean encontrado) {
        rotuloUsuarioIdentificado.getStyleClass()
                .removeAll("dados-usuario-encontrado", "dados-usuario-ausente");
        rotuloUsuarioIdentificado.getStyleClass()
                .add(encontrado ? "dados-usuario-encontrado" : "dados-usuario-ausente");
    }

    private void mostrarOfertaReserva(boolean mostrar) {
        botaoReservar.setVisible(mostrar);
    }

    private void iniciar(Task<?> tarefa, String nome) {
        Thread thread = new Thread(tarefa, nome);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onCancelar() {
        Navegador.irPara(Navegador.Tela.VISAO_GERAL);
    }

    /** CU 10 fluxo 3.1: ponto de extensão "Item indisponível" estendido pelo CU 11. */
    @FXML
    private void onReservarItem() {
        Item item = comboItem.getValue();
        Usuario usuario = usuarioIdentificado;
        String documento = campoDocumento.getText();
        Navegador.irPara(Navegador.Tela.RESERVA, controller ->
                ((ReservaController) controller).preencherDoEmprestimo(item, usuario, documento));
    }
}
