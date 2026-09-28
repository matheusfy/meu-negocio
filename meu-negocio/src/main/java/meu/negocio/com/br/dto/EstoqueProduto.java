package meu.negocio.com.br.dto;

import java.math.BigDecimal;

/**
 * Foto do estoque de um perfume: quantos frascos estão lacrados (podem ser vendidos cheios),
 * quantos estão abertos e com quanto ml, o custo médio ponderado do frasco e o valor total
 * parado em estoque (ver {@code planejamento_vendas_e_estoque.md}, §3.2).
 *
 * <p>{@code custoMedioFrasco} / {@code custoMedioMl} podem vir nulos quando não há compra
 * lançada nem preço de custo informado no perfume.
 */
public record EstoqueProduto(
    Long produtoId,
    int frascosComprados,
    int frascosVendidosCheios,
    int frascosAbertos,
    int frascosLacrados,
    BigDecimal mlNosAbertos,
    BigDecimal custoMedioFrasco,
    BigDecimal custoMedioMl,
    BigDecimal valorEstoque,
    int estoqueMinimo,
    boolean abaixoDoMinimo,
    boolean podeVenderCheio,
    boolean temCompras
) {
}
