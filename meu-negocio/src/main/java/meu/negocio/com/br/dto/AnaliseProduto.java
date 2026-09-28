package meu.negocio.com.br.dto;

import java.util.List;

/**
 * Resposta do endpoint {@code GET /api/v1/produtos/{id}/analise}: junta a comparação
 * entre os tamanhos de decante com os conselhos já traduzidos para linguagem simples.
 */
public record AnaliseProduto(
    Long produtoId,
    ComparacaoDecantes comparacao,
    List<Recomendacao> recomendacoes
) {
}
