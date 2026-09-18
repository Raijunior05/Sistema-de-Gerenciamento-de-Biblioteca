package br.univasf.bibliotech.view;

import br.univasf.bibliotech.App;
import br.univasf.bibliotech.service.RelatorioService;
import br.univasf.bibliotech.util.Sessao;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Tela inicial após o login. Reúne as entradas dos casos de uso. */
public class VisaoGeralController {

    @FXML private Label rodapeNome;
    @FXML private Label valorAtrasados;
    @FXML private Label valorEmprestados;
    @FXML private Label valorReservas;

    @FXML
    private void initialize() {
        if (Sessao.estaAutenticado()) {
            rodapeNome.setText(Sessao.getAdministrador().getNome());
        }
        carregarIndicadores();
    }

    private void carregarIndicadores() {
        Task<RelatorioService.Resumo> tarefa = new Task<>() {
            @Override
            protected RelatorioService.Resumo call() {
                return App.servicos().relatorios().resumoGeral();
            }
        };

        tarefa.setOnSucceeded(e -> {
            var r = tarefa.getValue();
            valorAtrasados.setText(String.valueOf(r.livrosAtrasados()));
            valorEmprestados.setText(String.valueOf(r.emprestimosAtivos()));
            valorReservas.setText(String.valueOf(r.reservasPendentes()));
        });

        tarefa.setOnFailed(e ->
                Alertas.erro("Não foi possível carregar os indicadores.", tarefa.getException()));

        Thread t = new Thread(tarefa, "indicadores");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onGerenciarUsuarios() {
        Navegador.irPara(Navegador.Tela.USUARIOS);
    }

    @FXML
    private void onRealizarEmprestimo() {
        Navegador.irPara(Navegador.Tela.EMPRESTIMO);
    }

    @FXML
    private void onConsultarEmprestimos() {
        Navegador.irPara(Navegador.Tela.RELATORIO);
    }

    @FXML
    private void onRealizarReserva() {
        Navegador.irPara(Navegador.Tela.RESERVA);
    }

    @FXML
    private void onGerarRelatorio() {
        Navegador.irPara(Navegador.Tela.RELATORIO);
    }

    @FXML
    private void onPesquisarAcervo() {
        Navegador.irPara(Navegador.Tela.ACERVO);
    }

    @FXML
    private void onCadastrarItem() {
        Navegador.irPara(Navegador.Tela.CADASTRO_ITEM);
    }

    @FXML
    private void onRealizarDevolucao() {
        Navegador.irPara(Navegador.Tela.DEVOLUCAO);
    }

    @FXML
    private void onSair() {
        if (Alertas.confirmar("Encerrar sessão", "Deseja sair do sistema?")) {
            Sessao.encerrar();
            Navegador.irPara(Navegador.Tela.LOGIN);
        }
    }
}