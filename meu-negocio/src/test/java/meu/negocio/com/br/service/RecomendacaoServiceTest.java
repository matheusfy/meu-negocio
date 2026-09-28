package meu.negocio.com.br.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import meu.negocio.com.br.dto.AnaliseViabilidade;
import meu.negocio.com.br.dto.ComparacaoDecantes;
import meu.negocio.com.br.dto.Recomendacao;
import meu.negocio.com.br.dto.Recomendacao.Nivel;

/**
 * Testes das regras que traduzem a viabilidade em conselhos ({@link RecomendacaoService}).
 * Cálculo puro: montam {@link ComparacaoDecantes} na mão, sem contexto Spring nem banco.
 */
class RecomendacaoServiceTest {

    private final RecomendacaoService service = new RecomendacaoService();

    /** Constrói uma análise só com os campos que as regras usam; o resto fica em zero. */
    private static AnaliseViabilidade av(long decanteId, String volumeMl, String precoVenda, String margem,
            int quantidade, String investimentoLote, String receitaTotal, String lucroTotal,
            String margemLote, String roi) {
        return new AnaliseViabilidade(
            1L, decanteId, new BigDecimal(volumeMl),
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            new BigDecimal(precoVenda),
            BigDecimal.ZERO,
            new BigDecimal(margem),
            quantidade,
            new BigDecimal(investimentoLote),
            new BigDecimal(receitaTotal),
            new BigDecimal(lucroTotal),
            new BigDecimal(margemLote),
            new BigDecimal(roi));
    }

    private static boolean temTitulo(List<Recomendacao> recs, String titulo) {
        return recs.stream().anyMatch(r -> r.titulo().equals(titulo));
    }

    @Test
    void produtoSemDecantesRecebeConviteParaCadastrar() {
        List<Recomendacao> recs = service.recomendar(new ComparacaoDecantes(1L, List.of(), null, null, null));

        assertEquals(1, recs.size());
        assertEquals(Nivel.INFO, recs.get(0).nivel());
        assertEquals("novo_decante", recs.get(0).acao());
    }

    @Test
    void comparacaoNulaNaoQuebra() {
        List<Recomendacao> recs = service.recomendar(null);

        assertEquals(1, recs.size());
        assertEquals("Cadastre um decante", recs.get(0).titulo());
    }

    @Test
    void melhorCenarioDestacaOTamanhoComMaiorMargemDeLote() {
        AnaliseViabilidade pequeno = av(1L, "3", "35", "49", 33, "592.60", "1155.00", "562.40", "48.69", "94.90");
        AnaliseViabilidade grande = av(2L, "10", "95", "41", 10, "562.00", "950.00", "388.00", "40.84", "69.04");
        ComparacaoDecantes comparacao = new ComparacaoDecantes(1L, List.of(pequeno, grande), 1L, 2L, 1L);

        List<Recomendacao> recs = service.recomendar(comparacao);

        assertTrue(temTitulo(recs, "Melhor tamanho: 3 ml"));
        assertTrue(recs.stream().anyMatch(
            r -> r.titulo().startsWith("Melhor tamanho") && r.nivel() == Nivel.BOM));
    }

    @Test
    void margemBaixaEmTodosOsTamanhosViraConselhoRuim() {
        AnaliseViabilidade a = av(1L, "3", "12", "20", 33, "400.00", "396.00", "-4.00", "-1.01", "-1.00");
        AnaliseViabilidade b = av(2L, "5", "18", "22", 20, "400.00", "360.00", "-40.00", "-11.11", "-10.00");
        ComparacaoDecantes comparacao = new ComparacaoDecantes(1L, List.of(a, b), 2L, 2L, 1L);

        List<Recomendacao> recs = service.recomendar(comparacao);

        assertTrue(temTitulo(recs, "Margem baixa em todos os tamanhos"));
        assertTrue(recs.stream().anyMatch(r -> r.nivel() == Nivel.RUIM));
    }

    @Test
    void pontoDeEquilibrioBomQuandoMetadeDoLotePagaOInvestimento() {
        AnaliseViabilidade a = av(1L, "5", "50", "45", 20, "500.00", "1000.00", "500.00", "50.00", "100.00");
        ComparacaoDecantes comparacao = new ComparacaoDecantes(1L, List.of(a), 1L, 1L, 1L);

        List<Recomendacao> recs = service.recomendar(comparacao);

        assertTrue(temTitulo(recs, "Paga o frasco cedo"));
    }

    @Test
    void pontoDeEquilibrioAcendeAlertaQuandoPrecisaVenderQuaseTudo() {
        AnaliseViabilidade a = av(1L, "5", "50", "30", 20, "900.00", "1000.00", "100.00", "10.00", "11.11");
        ComparacaoDecantes comparacao = new ComparacaoDecantes(1L, List.of(a), 1L, 1L, 1L);

        List<Recomendacao> recs = service.recomendar(comparacao);

        assertTrue(temTitulo(recs, "Precisa vender quase tudo para empatar"));
    }

    @Test
    void decanteSemPrecoDeVendaGeraAvisoAcionavel() {
        AnaliseViabilidade comPreco = av(1L, "5", "52", "45", 20, "574.00", "1040.00", "466.00", "44.81", "81.18");
        AnaliseViabilidade semPreco = av(2L, "10", "0", "0", 10, "562.00", "0.00", "-562.00", "-100.00", "-100.00");
        ComparacaoDecantes comparacao = new ComparacaoDecantes(1L, List.of(comPreco, semPreco), 1L, 1L, 1L);

        List<Recomendacao> recs = service.recomendar(comparacao);

        assertTrue(temTitulo(recs, "Falta o preço de venda"));
        assertTrue(recs.stream().anyMatch(r -> "editar_decante".equals(r.acao())));
    }

    @Test
    void explicaATrocaEntreTamanhoPequenoEGrande() {
        AnaliseViabilidade pequeno = av(1L, "3", "35", "49", 33, "592.60", "1155.00", "562.40", "48.69", "94.90");
        AnaliseViabilidade grande = av(2L, "10", "95", "41", 10, "562.00", "950.00", "388.00", "40.84", "69.04");
        ComparacaoDecantes comparacao = new ComparacaoDecantes(1L, List.of(pequeno, grande), 1L, 2L, 1L);

        List<Recomendacao> recs = service.recomendar(comparacao);

        assertTrue(temTitulo(recs, "Pequeno rende mais, grande dá menos trabalho"));
    }

    @Test
    void avisoDeLucroBrutoEstaSemprePresente() {
        AnaliseViabilidade a = av(1L, "5", "52", "45", 20, "574.00", "1040.00", "466.00", "44.81", "81.18");
        ComparacaoDecantes comparacao = new ComparacaoDecantes(1L, List.of(a), 1L, 1L, 1L);

        List<Recomendacao> recs = service.recomendar(comparacao);

        assertTrue(temTitulo(recs, "Isto é lucro bruto"));
    }
}
