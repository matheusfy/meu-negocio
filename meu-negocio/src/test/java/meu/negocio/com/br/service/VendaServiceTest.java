package meu.negocio.com.br.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import meu.negocio.com.br.entity.Venda;
import meu.negocio.com.br.entity.Venda.TipoVenda;

/**
 * Testes do congelamento de custo/receita/lucro da venda ({@link VendaService#aplicarSnapshot}).
 * Puro: não sobe contexto Spring nem banco.
 */
class VendaServiceTest {

    private final VendaService service = new VendaService(null, null, null);

    private static Venda venda(int quantidade, String precoUnitario) {
        Venda venda = new Venda(1L, null, TipoVenda.VIDRO_CHEIO, quantidade, new BigDecimal(precoUnitario), null, null);
        return venda;
    }

    @Test
    void congelaReceitaELucroDaVendaDeVidroCheio() {
        Venda v = venda(2, "350.00");

        service.aplicarSnapshot(v, new BigDecimal("100"), new BigDecimal("290.00"));

        assertEquals(new BigDecimal("100"), v.getVolumeUnitarioMl());
        assertEquals(new BigDecimal("290.00"), v.getCustoUnitario());
        assertEquals(new BigDecimal("700.00"), v.getReceitaTotal());
        assertEquals(new BigDecimal("120.00"), v.getLucroTotal()); // (350 - 290) x 2
    }

    @Test
    void lucroFicaNegativoQuandoVendeAbaixoDoCusto() {
        Venda v = venda(1, "250.00");

        service.aplicarSnapshot(v, new BigDecimal("100"), new BigDecimal("290.00"));

        assertEquals(new BigDecimal("-40.00"), v.getLucroTotal());
    }

    @Test
    void arredondaPrecoReceitaELucroParaDuasCasas() {
        Venda v = venda(3, "33.333");

        service.aplicarSnapshot(v, new BigDecimal("100"), new BigDecimal("10.00"));

        assertEquals(new BigDecimal("33.33"), v.getPrecoUnitario());
        assertEquals(new BigDecimal("99.99"), v.getReceitaTotal());
        assertEquals(new BigDecimal("69.99"), v.getLucroTotal()); // (33.33 - 10.00) x 3
    }

    @Test
    void custoNuloViraZeroNoSnapshot() {
        Venda v = venda(1, "80.00");

        service.aplicarSnapshot(v, new BigDecimal("50"), null);

        assertEquals(new BigDecimal("0.00"), v.getCustoUnitario());
        assertEquals(new BigDecimal("80.00"), v.getLucroTotal());
    }
}
