package br.univasf.bibliotech.util;

import java.util.function.Supplier;

/**
 * Delimita uma unidade de trabalho atomica sem expor JDBC ao service.
 *
 * <p>A implementacao real fica na camada dao; os testes unitarios usam
 * {@link #direta()}, que apenas executa a acao.</p>
 */
@FunctionalInterface
public interface Transacao {

    /** Executa a acao; qualquer excecao desfaz tudo o que foi gravado nela. */
    <T> T executar(Supplier<T> acao);

    /** Executa sem controle transacional, para testes com dubles. */
    static Transacao direta() {
        return new Transacao() {
            @Override
            public <T> T executar(Supplier<T> acao) {
                return acao.get();
            }
        };
    }
}
