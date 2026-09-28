package meu.negocio.com.br.dto;

import java.util.List;

/**
 * Comparação entre os decantes ativos de um mesmo produto, com os cenários destacados
 * (ver {@code planejamento_sistema_viabilidade_decantes.md}, §13).
 *
 * <p>Os campos {@code decante*} apontam o id do decante vencedor em cada critério; ficam
 * {@code null} quando não há decantes para comparar.
 */
public record ComparacaoDecantes(
    Long produtoId,
    List<AnaliseViabilidade> analises,
    Long decanteMelhorMargem,
    Long decanteMaiorLucroUnitario,
    Long decanteMelhorRetornoInvestimento
) {
}
