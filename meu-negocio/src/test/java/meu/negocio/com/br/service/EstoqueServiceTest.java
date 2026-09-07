package meu.negocio.com.br.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import meu.negocio.com.br.dto.EstoqueProduto;
import meu.negocio.com.br.entity.FrascoAberto;
import meu.negocio.com.br.entity.FrascoAberto.StatusFrasco;
import meu.negocio.com.br.entity.LoteCompra;
import meu.negocio.com.br.entity.Produto;

/**
 * Testes do cálculo puro de estoque ({@link EstoqueService#calcular} e {@code custoMedioFrasco}).
 * Não sobem contexto Spring nem banco: exercitam só as regras.
 */
class EstoqueServiceTest {

    private final EstoqueService service = new EstoqueService(null, null, null, null, null);

    private static Produto produto(String precoCusto, String volumeMl, int estoqueMinimo) {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setPrecoCusto(precoCusto == null ? null : new BigDecimal(precoCusto));
        produto.setVolumeMl(volumeMl == null ? null : new BigDecimal(volumeMl));
        produto.setEstoqueMinimo(estoqueMinimo);
        return produto;
    }

    private static LoteCompra lote(int quantidade, String precoUnitario, String custoAdicional) {
        return new LoteCompra(1L, null, quantidade, new BigDecimal(precoUnitario),
            new BigDecimal(custoAdicional), null, null);
    }

    private static FrascoAberto frasco(String mlRestante, StatusFrasco status) {
        FrascoAberto frasco = new FrascoAberto(1L, null, new BigDecimal("100.00"), null);
        frasco.setMlRestante(new BigDecimal(mlRestante));
        frasco.setStatus(status);
        return frasco;
    }

    @Test
    void custoMedioPonderaOsLotesPelaQuantidade() {
        // Exemplo do planejamento: 2 x 280 + 1 x 310 = 870 / 3 = 290,00
        BigDecimal medio = service.custoMedioFrasco(List.of(
            lote(2, "280.00", "0"),
            lote(1, "310.00", "0")));

        assertEquals(new BigDecimal("290.00"), medio);
    }

    @Test
    void custoAdicionalEhRateadoPelosFrascosDoLote() {
        // 100,00 + 20,00/2 = 110,00 por frasco
        BigDecimal medio = service.custoMedioFrasco(List.of(lote(2, "100.00", "20.00")));

        assertEquals(new BigDecimal("110.00"), medio);
    }

    @Test
    void semLoteOCustoMedioEhNulo() {
        assertNull(service.custoMedioFrasco(List.of()));
    }

    @Test
    void semLoteOEstoqueCaiNoPrecoDeCustoManualDoProduto() {
        EstoqueProduto estoque = service.calcular(produto("300.00", "100", 0), List.of(), List.of(), 0L);

        assertEquals(new BigDecimal("300.00"), estoque.custoMedioFrasco());
        assertEquals(new BigDecimal("3.0000"), estoque.custoMedioMl());
        assertFalse(estoque.temCompras());
        assertEquals(0, estoque.frascosLacrados());
        assertEquals(new BigDecimal("0.00"), estoque.valorEstoque());
    }

    @Test
    void abrirUmFrascoTiraUmDosLacrados() {
        EstoqueProduto estoque = service.calcular(
            produto(null, "100", 0),
            List.of(lote(3, "280.00", "0")),
            List.of(frasco("100.00", StatusFrasco.ABERTO)),
            0L);

        assertEquals(3, estoque.frascosComprados());
        assertEquals(1, estoque.frascosAbertos());
        assertEquals(2, estoque.frascosLacrados());
        assertTrue(estoque.podeVenderCheio());
    }

    @Test
    void mlNosAbertosSoContaFrascoComStatusAberto() {
        EstoqueProduto estoque = service.calcular(
            produto(null, "100", 0),
            List.of(lote(3, "280.00", "0")),
            List.of(
                frasco("40.00", StatusFrasco.ABERTO),
                frasco("0.00", StatusFrasco.ESGOTADO)),
            0L);

        assertEquals(new BigDecimal("40.00"), estoque.mlNosAbertos());
        assertEquals(1, estoque.frascosAbertos());
        // 3 comprados - 0 vendidos cheios - 2 abertos (inclui o esgotado) = 1 lacrado
        assertEquals(1, estoque.frascosLacrados());
    }

    @Test
    void avisaQuandoOsLacradosChegamNoEstoqueMinimo() {
        EstoqueProduto estoque = service.calcular(
            produto(null, "100", 2),
            List.of(lote(2, "100.00", "0")),
            List.of(),
            0L);

        assertEquals(2, estoque.frascosLacrados());
        assertTrue(estoque.abaixoDoMinimo());
    }

    @Test
    void estoqueMinimoZeroNuncaAcendeOAviso() {
        EstoqueProduto estoque = service.calcular(
            produto(null, "100", 0),
            List.of(),
            List.of(),
            0L);

        assertFalse(estoque.abaixoDoMinimo());
    }

    @Test
    void valorEmEstoqueSomaLacradosPeloCustoMedioEAbertosPeloCustoPorMl() {
        EstoqueProduto estoque = service.calcular(
            produto(null, "100", 0),
            List.of(lote(2, "280.00", "0"), lote(1, "310.00", "0")),
            List.of(frasco("95.00", StatusFrasco.ABERTO)),
            0L);

        // custo medio 290,00 -> custo/ml 2,9000
        assertEquals(new BigDecimal("290.00"), estoque.custoMedioFrasco());
        assertEquals(new BigDecimal("2.9000"), estoque.custoMedioMl());
        assertEquals(2, estoque.frascosLacrados());
        // 290,00 x 2  +  2,9000 x 95,00  =  580,00 + 275,50  =  855,50
        assertEquals(new BigDecimal("855.50"), estoque.valorEstoque());
    }

    @Test
    void frascosVendidosCheiosReduzemOsLacrados() {
        EstoqueProduto estoque = service.calcular(
            produto(null, "100", 0),
            List.of(lote(5, "280.00", "0")),
            List.of(),
            2L);

        assertEquals(5, estoque.frascosComprados());
        assertEquals(2, estoque.frascosVendidosCheios());
        assertEquals(3, estoque.frascosLacrados());
    }
}
