package meu.negocio.com.br.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Faturamento e lucro até o momento (ver {@code planejamento_vendas_e_estoque.md}, §3.5).
 * Todos os valores vêm dos snapshots congelados em cada {@code Venda} — não são recalculados.
 */
public record Resultado(
    LocalDate de,
    LocalDate ate,
    BigDecimal faturamento,
    BigDecimal custo,
    BigDecimal lucroBruto,
    BigDecimal margemMedia,
    int numVendas,
    BigDecimal ticketMedio,
    List<ResultadoPorPerfume> porPerfume
) {

    public record ResultadoPorPerfume(
        Long produtoId,
        String nome,
        BigDecimal faturamento,
        BigDecimal lucro,
        int numVendas
    ) {
    }
}
