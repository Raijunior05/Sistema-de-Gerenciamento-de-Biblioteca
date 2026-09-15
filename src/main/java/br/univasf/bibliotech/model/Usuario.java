package br.univasf.bibliotech.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Usuario da biblioteca (Casos de Uso 3 a 6).
 *
 * <h2>Origem dos campos</h2>
 * <p>Este model consolida duas fontes:</p>
 * <ul>
 *   <li><b>Prototipo Figma, tela "Cadastro Leitor"</b>: email, data de
 *       nascimento, telefone e senha.</li>
 *   <li><b>Documento de requisitos, CU 3 passo 03</b>: nome, email,
 *       CPF ou matricula, perfil de acesso e, para o perfil Administrador,
 *       login e senha.</li>
 * </ul>
 *
 * <p>O prototipo nao possuia os campos <i>nome</i>, <i>CPF/matricula</i> e
 * <i>perfil</i>, que sao obrigatorios pela especificacao: a identificacao do
 * CU 9 depende de CPF ou matricula, e a listagem do CU 6 exige o nome.</p>
 *
 * <p>Tambem diverge do prototipo o tratamento da senha: ela so existe para o
 * perfil {@link Perfil#ADMINISTRADOR}, ja que o Usuario comum nao acessa o
 * sistema. O campo "Confirme a senha" e validacao de formulario, nao atributo
 * de dominio, e por isso nao aparece aqui.</p>
 */
public class Usuario {

    private Long id;
    private String nome;
    private String email;
    private String cpf;
    private String matricula;
    private String telefone;
    private LocalDate nascimento;
    private Perfil perfil = Perfil.USUARIO;
    private String login;
    private String senhaHash;
    private boolean ativo = true;
    private LocalDateTime criadoEm;

    public Usuario() {
    }

    public Usuario(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    /**
     * Identificacao usada no CU 9: o Usuario informa CPF ou matricula.
     *
     * @return a matricula quando houver, senao o CPF
     */
    public String getIdentificacao() {
        return matricula != null && !matricula.isBlank() ? matricula : cpf;
    }

    public boolean isAdministrador() {
        return perfil == Perfil.ADMINISTRADOR;
    }

    /** Exibicao usada nas tabelas do prototipo (coluna "Leitor"). */
    public String getResumo() {
        return nome + (getIdentificacao() != null ? " (" + getIdentificacao() + ")" : "");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public LocalDate getNascimento() {
        return nascimento;
    }

    public void setNascimento(LocalDate nascimento) {
        this.nascimento = nascimento;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario outro)) {
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
        return nome;
    }
}
