package br.univasf.bibliotech.view;

import br.univasf.bibliotech.model.Usuario;

/** CU 9 passo 05: texto com os dados do Usuario identificado. */
final class DadosUsuario {

    private DadosUsuario() {
    }

    static String formatar(Usuario usuario) {
        StringBuilder texto = new StringBuilder("Usuário: ").append(usuario.getNome());
        if (usuario.getCpf() != null && !usuario.getCpf().isBlank()) {
            texto.append("\nCPF: ").append(usuario.getCpf());
        }
        if (usuario.getMatricula() != null && !usuario.getMatricula().isBlank()) {
            texto.append("\nMatrícula: ").append(usuario.getMatricula());
        }
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            texto.append("\nE-mail: ").append(usuario.getEmail());
        }
        return texto.toString();
    }
}
