package meu.negocio.com.br.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import meu.negocio.com.br.dto.AnaliseViabilidade;
import meu.negocio.com.br.dto.ComparacaoDecantes;
import meu.negocio.com.br.dto.Recomendacao;
import meu.negocio.com.br.dto.Recomendacao.Nivel;

/**
 * Traduz o resultado numérico da {@link AnaliseViabilidadeService} em conselhos curtos,
 * em linguagem de dona de negócio. Cálculo puro: não acessa banco.
 *
 * <p>As faixas (margem confortável, ponto de equilíbrio) são referências de mercado para
 * venda de decantes, não regras fixas — ver {@code planejamento_frontend.md}.
 */
@Service
public class RecomendacaoService {

    /** Margem a partir da qual o cenário é confortável (em pontos percentuais). */
    private static final BigDecimal MARGEM_CONFORTAVEL = new BigDecimal("40");
    /** Abaixo desta margem o cenário é considerado ruim. */
    private static final BigDecimal MARGEM_MINIMA = new BigDecimal("25");
    /** Fração do lote que, bastando para pagar tudo, é sinal bom. */
    private static final BigDecimal EQUILIBRIO_BOM = new BigDecimal("0.50");
    /** Fração do lote que, sendo necessária para empatar, acende alerta. */
    private static final BigDecimal EQUILIBRIO_ALERTA = new BigDecimal("0.65");

    /** Gera a lista de conselhos para a comparação de decantes de um produto. */
    public List<Recomendacao> recomendar(ComparacaoDecantes comparacao) {
        if (comparacao == null || comparacao.analises() == null || comparacao.analises().isEmpty()) {
            return List.of(new Recomendacao(Nivel.INFO,
                "Cadastre um decante",
                "Configure pelo menos um tamanho de decante para este perfume para ver se compensa fracionar.",
                "novo_decante"));
        }

        List<AnaliseViabilidade> analises = comparacao.analises();
        List<Recomendacao> recs = new ArrayList<>();

        // 1. Decantes sem preço de venda travam o cálculo — avisar primeiro.
        for (AnaliseViabilidade a : analises) {
            if (a.precoVenda() == null || a.precoVenda().signum() == 0) {
                recs.add(new Recomendacao(Nivel.INFO,
                    "Falta o preço de venda",
                    "O decante de " + ml(a.volumeMl()) + " está sem preço de venda. Defina para ver o lucro.",
                    "editar_decante"));
            }
        }

        AnaliseViabilidade melhor = analises.stream()
            .max(Comparator.comparing(AnaliseViabilidade::margemLote))
            .orElseThrow();

        // 2. Melhor cenário.
        recs.add(new Recomendacao(nivelPorMargem(melhor.margemLote()),
            "Melhor tamanho: " + ml(melhor.volumeMl()),
            "Fracionar em " + ml(melhor.volumeMl()) + " dá margem de " + pct(melhor.margem())
                + " por unidade. No frasco inteiro você investe " + reais(melhor.investimentoLote())
                + ", fatura " + reais(melhor.receitaTotal()) + " e lucra " + reais(melhor.lucroTotal())
                + " (retorno de " + pct(melhor.roi()) + ").",
            "ver_comparacao"));

        // 3. Saúde da margem.
        boolean todasRuins = analises.stream()
            .allMatch(a -> a.margem().compareTo(MARGEM_MINIMA) < 0);
        if (todasRuins) {
            recs.add(new Recomendacao(Nivel.RUIM,
                "Margem baixa em todos os tamanhos",
                "Nenhum tamanho passa de " + pct(MARGEM_MINIMA) + " de margem. Reveja os preços de venda "
                    + "ou considere outro perfume.",
                "editar_perfume"));
        } else if (melhor.margem().compareTo(MARGEM_CONFORTAVEL) < 0) {
            recs.add(new Recomendacao(Nivel.ATENCAO,
                "Margem apertada",
                "A melhor margem entre os tamanhos é " + pct(melhor.margem())
                    + ". Dá lucro, mas sobra pouco para imprevistos — um reajuste pequeno no preço já ajuda.",
                "editar_decante"));
        }

        // 4. Ponto de equilíbrio do melhor cenário.
        if (temPreco(melhor) && melhor.quantidadeDecantes() > 0) {
            BigDecimal equilibrio = melhor.investimentoLote()
                .divide(melhor.precoVenda(), 0, RoundingMode.CEILING);
            BigDecimal fracao = equilibrio.divide(
                BigDecimal.valueOf(melhor.quantidadeDecantes()), 2, RoundingMode.HALF_UP);
            String quantos = equilibrio.toBigInteger() + " dos " + melhor.quantidadeDecantes();
            if (fracao.compareTo(EQUILIBRIO_BOM) <= 0) {
                recs.add(new Recomendacao(Nivel.BOM,
                    "Paga o frasco cedo",
                    "Vendendo " + quantos + " decantes de " + ml(melhor.volumeMl())
                        + " você já cobre todo o investimento. O restante é lucro.",
                    null));
            } else if (fracao.compareTo(EQUILIBRIO_ALERTA) >= 0) {
                recs.add(new Recomendacao(Nivel.ATENCAO,
                    "Precisa vender quase tudo para empatar",
                    "É preciso vender " + quantos + " decantes de " + ml(melhor.volumeMl())
                        + " só para empatar. Se o frasco não vender inteiro, o lucro cai rápido.",
                    null));
            }
        }

        // 5. Troca entre tamanho pequeno e grande.
        Optional<AnaliseViabilidade> melhorMargem = porId(analises, comparacao.decanteMelhorMargem());
        Optional<AnaliseViabilidade> maiorLucro = porId(analises, comparacao.decanteMaiorLucroUnitario());
        if (melhorMargem.isPresent() && maiorLucro.isPresent()
                && !melhorMargem.get().decanteId().equals(maiorLucro.get().decanteId())) {
            recs.add(new Recomendacao(Nivel.INFO,
                "Pequeno rende mais, grande dá menos trabalho",
                "O decante de " + ml(melhorMargem.get().volumeMl()) + " rende mais por real investido; "
                    + "o de " + ml(maiorLucro.get().volumeMl()) + " dá lucro maior em cada venda e menos "
                    + "trabalho de embalar e enviar. Escolha pelo seu tempo e pela procura.",
                null));
        }

        // 6. Custos que ainda não entram na conta.
        recs.add(new Recomendacao(Nivel.INFO,
            "Isto é lucro bruto",
            "As contas ainda não incluem taxa de marketplace, frete e embalagem de envio. "
                + "Se você vende por aplicativo, desconte isso da margem.",
            null));

        return recs;
    }

    private static boolean temPreco(AnaliseViabilidade a) {
        return a.precoVenda() != null && a.precoVenda().signum() > 0;
    }

    private static Optional<AnaliseViabilidade> porId(List<AnaliseViabilidade> analises, Long decanteId) {
        if (decanteId == null) {
            return Optional.empty();
        }
        return analises.stream().filter(a -> decanteId.equals(a.decanteId())).findFirst();
    }

    private static Nivel nivelPorMargem(BigDecimal margem) {
        if (margem.compareTo(MARGEM_CONFORTAVEL) >= 0) {
            return Nivel.BOM;
        }
        if (margem.compareTo(MARGEM_MINIMA) >= 0) {
            return Nivel.ATENCAO;
        }
        return Nivel.RUIM;
    }

    private static String pct(BigDecimal valor) {
        return valor.setScale(0, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private static String reais(BigDecimal valor) {
        return "R$ " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static String ml(BigDecimal volumeMl) {
        BigDecimal v = volumeMl.stripTrailingZeros();
        if (v.scale() < 0) {
            v = v.setScale(0);
        }
        return v.toPlainString() + " ml";
    }
}
