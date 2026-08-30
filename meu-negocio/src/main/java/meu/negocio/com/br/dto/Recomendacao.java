package meu.negocio.com.br.dto;

/**
 * Conselho curto, em linguagem de dona de negócio, derivado do cálculo de viabilidade.
 * Montado pelo {@code RecomendacaoService}; não guarda nada no banco.
 *
 * @param nivel  cor/gravidade do conselho ({@link Nivel})
 * @param titulo frase curta que resume o ponto
 * @param texto  explicação em uma ou duas linhas, sem jargão
 * @param acao   slug opcional que o front usa para oferecer um atalho
 *               (ex.: {@code "ver_comparacao"}, {@code "editar_decante"}); {@code null} quando não há ação
 */
public record Recomendacao(Nivel nivel, String titulo, String texto, String acao) {

    /** Bom (verde), atenção (dourado), ruim (vermelho) ou apenas informativo. */
    public enum Nivel { BOM, ATENCAO, RUIM, INFO }
}
