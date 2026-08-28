package meu.negocio.com.br.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import meu.negocio.com.br.dto.AnaliseViabilidade;
import meu.negocio.com.br.entity.Decante;
import meu.negocio.com.br.entity.Produto;

/**
 * Testes do cálculo puro de viabilidade ({@link AnaliseViabilidadeService#analisar}).
 * Não sobem contexto Spring nem banco: exercitam só as regras de negócio.
 */
class AnaliseViabilidadeServiceTest {

    private final AnaliseViabilidadeService service = new AnaliseViabilidadeService(null, null, null);

    private static Produto produto(String precoCusto, String volumeMl) {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setPrecoCusto(precoCusto == null ? null : new BigDecimal(precoCusto));
        produto.setVolumeMl(volumeMl == null ? null : new BigDecimal(volumeMl));
        return produto;
    }

    private static Decante decante(Long id, Long produtoId, String volumeMl, String precoVenda,
            String frasco, String seringa, String etiqueta) {
        Decante decante = new Decante();
        decante.setId(id);
        decante.setProdutoId(produtoId);
        decante.setVolumeMl(new BigDecimal(volumeMl));
        decante.setPrecoVenda(new BigDecimal(precoVenda));
        decante.setCustoEmbalagem(new BigDecimal(frasco));
        decante.setCustoSeringa(new BigDecimal(seringa));
        decante.setCustoEtiqueta(new BigDecimal(etiqueta));
        return decante;
    }

    @Test
    void reproduzOCenarioDoPlanejamentoParaDecanteDe5ml() {
        Produto produto = produto("300.00", "100");
        Decante decante = decante(10L, 1L, "5", "30.00", "2.00", "0.50", "0.20");

        AnaliseViabilidade analise = service.analisar(produto, decante);

        assertEquals(new BigDecimal("3.0000"), analise.custoPorMl());
        assertEquals(new BigDecimal("15.00"), analise.custoProdutoNoDecante());
        assertEquals(new BigDecimal("2.70"), analise.custoEmbalagem());
        assertEquals(new BigDecimal("17.70"), analise.custoTotal());
        assertEquals(new BigDecimal("12.30"), analise.lucroUnitario());
        assertEquals(new BigDecimal("41.00"), analise.margem());
        assertEquals(20, analise.quantidadeDecantes());
        assertEquals(new BigDecimal("354.00"), analise.investimentoLote());
        assertEquals(new BigDecimal("600.00"), analise.receitaTotal());
        assertEquals(new BigDecimal("246.00"), analise.lucroTotal());
        assertEquals(new BigDecimal("41.00"), analise.margemLote());
        assertEquals(1L, analise.produtoId());
        assertEquals(10L, analise.decanteId());
    }

    @Test
    void consideraSobraDeProdutoQuandoOVolumeNaoDivideExato() {
        Produto produto = produto("300.00", "100");
        Decante decante = decante(11L, 1L, "3", "20.00", "1.50", "0.50", "0.20");

        AnaliseViabilidade analise = service.analisar(produto, decante);

        // Unitário: bate com a tabela do §13 (3 ml -> custo 11,20 / lucro 8,80 / margem 44%).
        assertEquals(new BigDecimal("9.00"), analise.custoProdutoNoDecante());
        assertEquals(new BigDecimal("2.20"), analise.custoEmbalagem());
        assertEquals(new BigDecimal("11.20"), analise.custoTotal());
        assertEquals(new BigDecimal("8.80"), analise.lucroUnitario());
        assertEquals(new BigDecimal("44.00"), analise.margem());
        // 100 / 3 = 33 decantes (1 ml de sobra), investimento usa o perfume inteiro.
        assertEquals(33, analise.quantidadeDecantes());
        assertEquals(new BigDecimal("372.60"), analise.investimentoLote());
        assertEquals(new BigDecimal("660.00"), analise.receitaTotal());
        assertEquals(new BigDecimal("287.40"), analise.lucroTotal());
        assertEquals(new BigDecimal("43.55"), analise.margemLote());
    }

    @Test
    void arredondaOCustoPorMlComQuatroCasas() {
        Produto produto = produto("100.00", "30");
        Decante decante = decante(12L, 1L, "10", "25.00", "1.00", "0.50", "0.20");

        AnaliseViabilidade analise = service.analisar(produto, decante);

        // 100 / 30 = 3,33333... -> 3,3333
        assertEquals(new BigDecimal("3.3333"), analise.custoPorMl());
        // 3,3333 * 10 = 33,333 -> 33,33
        assertEquals(new BigDecimal("33.33"), analise.custoProdutoNoDecante());
        assertEquals(3, analise.quantidadeDecantes());
    }

    @Test
    void margemFicaZeroQuandoPrecoDeVendaEhZero() {
        Produto produto = produto("300.00", "100");
        Decante decante = decante(13L, 1L, "5", "0", "2.00", "0.50", "0.20");

        AnaliseViabilidade analise = service.analisar(produto, decante);

        assertEquals(new BigDecimal("0.00"), analise.margem());
        assertEquals(new BigDecimal("0.00"), analise.margemLote());
        assertEquals(new BigDecimal("0.00"), analise.receitaTotal());
        assertEquals(new BigDecimal("-17.70"), analise.lucroUnitario());
    }

    @Test
    void rejeitaDecanteDeOutroProduto() {
        Produto produto = produto("300.00", "100");
        Decante decante = decante(14L, 99L, "5", "30.00", "2.00", "0.50", "0.20");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
            () -> service.analisar(produto, decante));
        assertEquals("O decante não pertence ao produto informado", erro.getMessage());
    }

    @Test
    void rejeitaProdutoSemVolume() {
        Produto produto = produto("300.00", null);
        Decante decante = decante(15L, 1L, "5", "30.00", "2.00", "0.50", "0.20");

        assertThrows(IllegalArgumentException.class, () -> service.analisar(produto, decante));
    }

    @Test
    void rejeitaProdutoComVolumeZero() {
        Produto produto = produto("300.00", "0");
        Decante decante = decante(16L, 1L, "5", "30.00", "2.00", "0.50", "0.20");

        assertThrows(IllegalArgumentException.class, () -> service.analisar(produto, decante));
    }

    @Test
    void rejeitaProdutoSemPrecoDeCusto() {
        Produto produto = produto(null, "100");
        Decante decante = decante(17L, 1L, "5", "30.00", "2.00", "0.50", "0.20");

        assertThrows(IllegalArgumentException.class, () -> service.analisar(produto, decante));
    }
}
