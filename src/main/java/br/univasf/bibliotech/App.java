package br.univasf.bibliotech;

import br.univasf.bibliotech.dao.EmprestimoDAOPostgres;
import br.univasf.bibliotech.dao.ItemDAOPostgres;
import br.univasf.bibliotech.dao.ReservaDAOPostgres;
import br.univasf.bibliotech.dao.UsuarioDAOPostgres;
import br.univasf.bibliotech.service.AutenticacaoService;
import br.univasf.bibliotech.service.EmprestimoService;
import br.univasf.bibliotech.service.ItemService;
import br.univasf.bibliotech.service.RelatorioService;
import br.univasf.bibliotech.service.ReservaService;
import br.univasf.bibliotech.service.UsuarioService;
import br.univasf.bibliotech.util.Configuracao;
import br.univasf.bibliotech.util.ConnectionFactory;
import br.univasf.bibliotech.view.Navegador;
import javafx.application.Application;
import javafx.stage.Stage;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

/**
 * Ponto de entrada do BiblioTech.
 *
 * <p>Diferente de uma aplicacao web, aqui nao ha container nem injecao de
 * dependencia automatica: as instancias sao criadas uma unica vez neste
 * metodo e passadas adiante pelos construtores. Isso deixa explicito quem
 * depende de quem e mantem os services testaveis com mocks.</p>
 */
public class App extends Application {

    /** Senha do administrador criado na primeira execucao. */
    private static final String SENHA_ADMIN_PADRAO = "admin123";

    private static Servicos servicos;

    /** Agrupa os services para circular entre os controllers. */
    public record Servicos(AutenticacaoService autenticacao,
                           UsuarioService usuarios,
                           ItemService itens,
                           EmprestimoService emprestimos,
                           ReservaService reservas,
                           RelatorioService relatorios) {
    }

    public static Servicos servicos() {
        return servicos;
    }

    @Override
    public void init() {
        DataSource ds = ConnectionFactory.getDataSource();

        // 1. Schema sempre atualizado, em qualquer maquina do grupo.
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();

        // 2. Montagem das dependencias (view -> service -> dao -> banco).
        var usuarioDAO = new UsuarioDAOPostgres(ds);
        var itemDAO = new ItemDAOPostgres(ds);
        var emprestimoDAO = new EmprestimoDAOPostgres(ds);
        var reservaDAO = new ReservaDAOPostgres(ds);

        var autenticacao = new AutenticacaoService(usuarioDAO);
        var emprestimos = new EmprestimoService(
                emprestimoDAO, itemDAO, reservaDAO,
                Configuracao.inteiro("regra.limiteEmprestimosPorUsuario"),
                Configuracao.inteiro("regra.diasPrazoEmprestimo"));
        var reservas = new ReservaService(
                reservaDAO, itemDAO, Configuracao.inteiro("regra.diasValidadeReserva"));

        servicos = new Servicos(
                autenticacao,
                new UsuarioService(usuarioDAO),
                new ItemService(itemDAO),
                emprestimos,
                reservas,
                new RelatorioService(emprestimoDAO, reservaDAO, usuarioDAO));

        // 3. Administrador de instalacao, se ainda nao houver nenhum.
        autenticacao.garantirAdministradorInicial(SENHA_ADMIN_PADRAO).ifPresent(admin ->
                System.out.println("""

                        ==========================================================
                         Administrador inicial criado.
                         Acesso: admin@bibliotech.local
                         Senha:  %s
                         Altere a senha no primeiro acesso.
                        ==========================================================
                        """.formatted(SENHA_ADMIN_PADRAO)));

        // 4. Rotinas de manutencao diaria.
        emprestimos.atualizarAtrasos();
        reservas.expirarVencidas();
    }

    @Override
    public void start(Stage palco) {
        Navegador.iniciar(palco);
        Navegador.irPara(Navegador.Tela.LOGIN);
    }

    @Override
    public void stop() {
        ConnectionFactory.encerrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
