package br.univasf.bibliotech.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Emprestimo de um item a um usuario (Casos de Uso 10 e 12).
 *
 * <h2>Divergencia do prototipo</h2>
 * <p>A tela "Cadastro Emprestimo" do Figma pede o nome do titulo e o e-mail do
 * leitor como texto livre, e deixa o Administrador digitar as duas datas.
 * A especificacao determina outro comportamento:</p>
 * <ul>
 *   <li>o Usuario e identificado por CPF ou matricula (CU 9), nao por e-mail;</li>
 *   <li>o item e selecionado do acervo, nao digitado;</li>
 *   <li>a data prevista e calculada pelo sistema (CU 10 passo 08), nao digitada.</li>
 * </ul>
 *
 * <p>CU 12, fluxo 5.1: registra apenas os dias de atraso.</p>
 */
public class Emprestimo {

    private Long id;
    private String codigo;
    private Usuario usuario;
    private Item item;
    private LocalDate dataEmprestimo = LocalDate.now();
    private LocalDate dataPrevista;
    private LocalDate dataDevolucao;
    private StatusEmprestimo status = StatusEmprestimo.EM_ANDAMENTO;
    private int diasAtraso;
    private Usuario registradoPor;

    public Emprestimo() {
    }

    public Emprestimo(Usuario usuario, Item item, LocalDate dataPrevista) {
        this.usuario = usuario;
        this.item = item;
        this.dataPrevista = dataPrevista;
    }

    /**
     * CU 12 passo 05 e fluxo alternativo 5.1.
     *
     * <p>Compara a data de referencia com a data prevista. Para um emprestimo
     * ja concluido usa a data real de devolucao; para um em aberto usa a data
     * informada, normalmente {@code LocalDate.now()}.</p>
     *
     * @param referencia data com a qual comparar o prazo
     * @return numero de dias de atraso, ou zero se estiver no prazo
     */
    public long calcularDiasAtraso(LocalDate referencia) {
        LocalDate base = dataDevolucao != null ? dataDevolucao : referencia;
        if (base == null || dataPrevista == null || !base.isAfter(dataPrevista)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dataPrevista, base);
    }

    /** Verdadeiro quando o emprestimo esta aberto e o prazo ja venceu. */
    public boolean estaAtrasado(LocalDate referencia) {
        return status.estaAberto() && calcularDiasAtraso(referencia) > 0;
    }

    /** Dias restantes ate o vencimento. Negativo indica atraso. */
    public long getDiasRestantes(LocalDate referencia) {
        if (dataPrevista == null || referencia == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(referencia, dataPrevista);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public LocalDate getDataEmprestimo() {
        return dataEmprestimo;
    }

    public void setDataEmprestimo(LocalDate dataEmprestimo) {
        this.dataEmprestimo = dataEmprestimo;
    }

    public LocalDate getDataPrevista() {
        return dataPrevista;
    }

    public void setDataPrevista(LocalDate dataPrevista) {
        this.dataPrevista = dataPrevista;
    }

    public LocalDate getDataDevolucao() {
        return dataDevolucao;
    }

    public void setDataDevolucao(LocalDate dataDevolucao) {
        this.dataDevolucao = dataDevolucao;
    }

    public StatusEmprestimo getStatus() {
        return status;
    }

    public void setStatus(StatusEmprestimo status) {
        this.status = status;
    }

    public int getDiasAtraso() {
        return diasAtraso;
    }

    public void setDiasAtraso(int diasAtraso) {
        this.diasAtraso = diasAtraso;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Usuario registradoPor) {
        this.registradoPor = registradoPor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Emprestimo outro)) {
            return false;
        }
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return codigo;
    }
}
