package meu.negocio.com.br.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import meu.negocio.com.br.dto.Resultado;
import meu.negocio.com.br.entity.Venda;
import meu.negocio.com.br.entity.Venda.TipoVenda;

/**
 * Testes da agregação de faturamento e lucro ({@link ResultadoService#calcular}).
 * Puro: monta as vendas na mão, sem contexto Spring nem banco.
 */
class ResultadoServiceTest {

    private final ResultadoService service = new ResultadoService(null, null);

    private static Venda venda(long produtoId, int quantidade, String receitaTotal, String custoUnitario,
            String lucroTotal) {
        Venda venda = new Venda();
        venda.setProdutoId(produtoId);
        venda.setTipo(TipoVenda.VIDRO_CHEIO);
        venda.setQuantidade(quantidade);
        venda.setReceitaTotal(new BigDecimal(receitaTotal));
        venda.setCustoUnitario(new BigDecimal(custoUnitario));
        venda.setLucroTotal(new BigDecimal(lucroTotal));
        venda.setAtivo(true);
        return venda;
    }

    @Test
    void semVendasZeraTudo() {
        Resultado r = service.calcular(List.of(), Map.of(), null, null);

        assertEquals(new BigDecimal("0.00"), r.faturamento());
        assertEquals(new BigDecimal("0.00"), r.custo());
        assertEquals(new BigDecimal("0.00"), r.lucroBruto());
        assertEquals(new BigDecimal("0.00"), r.margemMedia());
        assertEquals(new BigDecimal("0.00"), r.ticketMedio());
        assertEquals(0, r.numVendas());
        assertEquals(0, r.porPerfume().size());
    }

    @Test
    void somaFaturamentoCustoLucroTicketEMargem() {
        // v1: 2 frascos, receita 700, custo unit 290 (custo 580), lucro 120
        // v2: 1 frasco,  receita 350, custo unit 280 (custo 280), lucro 70
        Resultado r = service.calcular(
            List.of(
                venda(1L, 2, "700.00", "290.00", "120.00"),
                venda(1L, 1, "350.00", "280.00", "70.00")),
            Map.of(1L, "Bright"),
            null, null);

        assertEquals(new BigDecimal("1050.00"), r.faturamento());
        assertEquals(new BigDecimal("860.00"), r.custo());
        assertEquals(new BigDecimal("190.00"), r.lucroBruto());
        assertEquals(2, r.numVendas());
        assertEquals(new BigDecimal("525.00"), r.ticketMedio());
        assertEquals(new BigDecimal("18.10"), r.margemMedia()); // 190 / 1050 x 100
    }

    @Test
    void agrupaPorPerfumeEOrdenaPeloLucroDecrescente() {
        Resultado r = service.calcular(
            List.of(
                venda(1L, 1, "100.00", "60.00", "40.00"),
                venda(2L, 1, "300.00", "180.00", "120.00"),
                venda(1L, 1, "100.00", "70.00", "30.00")),
            Map.of(1L, "Barato", 2L, "Caro"),
            null, null);

        assertEquals(2, r.porPerfume().size());
        // Caro (lucro 120) vem antes de Barato (lucro 70)
        assertEquals("Caro", r.porPerfume().get(0).nome());
        assertEquals(new BigDecimal("120.00"), r.porPerfume().get(0).lucro());
        assertEquals("Barato", r.porPerfume().get(1).nome());
        assertEquals(new BigDecimal("200.00"), r.porPerfume().get(1).faturamento());
        assertEquals(new BigDecimal("70.00"), r.porPerfume().get(1).lucro());
        assertEquals(2, r.porPerfume().get(1).numVendas());
    }

    @Test
    void usaOsValoresCongeladosDaVendaSemRecalcular() {
        // lucroTotal deliberadamente "errado" em relação a receita/custo: a agregação respeita o snapshot.
        Resultado r = service.calcular(
            List.of(venda(1L, 1, "100.00", "40.00", "999.00")),
            Map.of(1L, "X"),
            null, null);

        assertEquals(new BigDecimal("100.00"), r.faturamento());
        assertEquals(new BigDecimal("40.00"), r.custo());
        assertEquals(new BigDecimal("999.00"), r.porPerfume().get(0).lucro());
    }
}
