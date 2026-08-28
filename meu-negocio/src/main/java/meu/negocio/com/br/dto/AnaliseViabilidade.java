package meu.negocio.com.br.dto;

import java.math.BigDecimal;

/**
 * Resultado do cálculo de viabilidade de um {@code Decante} a partir de um {@code Produto}.
 *
 * <p>Valores unitários referem-se a um único decante; valores de lote consideram o
 * perfume inteiro como investimento (ver {@code planejamento_sistema_viabilidade_decantes.md}, §12).
 */
public record AnaliseViabilidade(
    Long produtoId,
    Long decanteId,
    BigDecimal custoPorMl,
    BigDecimal custoProdutoNoDecante,
    BigDecimal custoEmbalagem,
    BigDecimal custoTotal,
    BigDecimal precoVenda,
    BigDecimal lucroUnitario,
    BigDecimal margem,
    int quantidadeDecantes,
    BigDecimal investimentoLote,
    BigDecimal receitaTotal,
    BigDecimal lucroTotal,
    BigDecimal margemLote
) {
}
