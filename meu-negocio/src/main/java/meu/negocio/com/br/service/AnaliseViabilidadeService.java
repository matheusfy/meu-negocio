package meu.negocio.com.br.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import meu.negocio.com.br.dto.AnaliseViabilidade;
import meu.negocio.com.br.dto.ComparacaoDecantes;
import meu.negocio.com.br.entity.Decante;
import meu.negocio.com.br.entity.Produto;
import meu.negocio.com.br.repository.DecanteRepository;

/**
 * Coração do simulador: calcula a viabilidade de transformar um {@link Produto} em
 * {@link Decante}s de determinado tamanho. Não persiste nada — apenas calcula.
 */
@Service
public class AnaliseViabilidadeService {

    /** Casas decimais para o custo por ml (mais preciso que dinheiro para não acumular erro). */
    private static final int ESCALA_CUSTO_ML = 4;
    /** Casas decimais para valores monetários (R$). */
    private static final int ESCALA_DINHEIRO = 2;
    /** Casas decimais para percentuais de margem. */
    private static final int ESCALA_PERCENTUAL = 2;
    private static final BigDecimal CEM = new BigDecimal("100");

    private final ProdutoService produtoService;
    private final DecanteService decanteService;
    private final DecanteRepository decanteRepository;

    public AnaliseViabilidadeService(ProdutoService produtoService, DecanteService decanteService,
            DecanteRepository decanteRepository) {
        this.produtoService = produtoService;
        this.decanteService = decanteService;
        this.decanteRepository = decanteRepository;
    }

    /** Carrega o decante (e seu produto) e devolve a análise. */
    public AnaliseViabilidade analisarDecante(Long decanteId) {
        Decante decante = decanteService.findById(decanteId);
        Produto produto = produtoService.findById(decante.getProdutoId());
        return analisar(produto, decante);
    }

    /** Compara todos os decantes ativos de um produto e destaca os melhores cenários. */
    public ComparacaoDecantes compararProduto(Long produtoId) {
        Produto produto = produtoService.findById(produtoId);
        List<AnaliseViabilidade> analises = decanteRepository.findByProdutoIdAndAtivoTrue(produtoId).stream()
            .map(decante -> analisar(produto, decante))
            .toList();

        return new ComparacaoDecantes(
            produtoId,
            analises,
            idDoMelhor(analises, Comparator.comparing(AnaliseViabilidade::margem)),
            idDoMelhor(analises, Comparator.comparing(AnaliseViabilidade::lucroUnitario)),
            idDoMelhor(analises, Comparator.comparing(AnaliseViabilidadeService::retornoInvestimento))
        );
    }

    /**
     * Cálculo puro de viabilidade de um decante a partir de um produto.
     * Testável sem banco: recebe as entidades já carregadas.
     */
    public AnaliseViabilidade analisar(Produto produto, Decante decante) {
        BigDecimal precoCusto = exigirPreenchido(produto.getPrecoCusto(), "Preço de custo do produto");
        BigDecimal volumeProduto = exigirPositivo(produto.getVolumeMl(), "Volume do produto");
        BigDecimal volumeDecante = exigirPositivo(decante.getVolumeMl(), "Volume do decante");
        exigirMesmoProduto(produto, decante);

        BigDecimal custoPorMl = precoCusto.divide(volumeProduto, ESCALA_CUSTO_ML, RoundingMode.HALF_UP);
        BigDecimal custoProdutoNoDecante = dinheiro(custoPorMl.multiply(volumeDecante));
        BigDecimal custoEmbalagem = dinheiro(decante.custoEmbalagemTotal());
        BigDecimal custoTotal = dinheiro(custoProdutoNoDecante.add(custoEmbalagem));
        BigDecimal precoVenda = dinheiro(nvl(decante.getPrecoVenda()));
        BigDecimal lucroUnitario = dinheiro(precoVenda.subtract(custoTotal));
        BigDecimal margem = percentual(lucroUnitario, precoVenda);

        int quantidadeDecantes = volumeProduto.divideToIntegralValue(volumeDecante).intValueExact();
        BigDecimal custoEmbalagemLote = custoEmbalagem.multiply(BigDecimal.valueOf(quantidadeDecantes));
        BigDecimal investimentoLote = dinheiro(precoCusto.add(custoEmbalagemLote));
        BigDecimal receitaTotal = dinheiro(precoVenda.multiply(BigDecimal.valueOf(quantidadeDecantes)));
        BigDecimal lucroTotal = dinheiro(receitaTotal.subtract(investimentoLote));
        BigDecimal margemLote = percentual(lucroTotal, receitaTotal);

        return new AnaliseViabilidade(
            produto.getId(),
            decante.getId(),
            custoPorMl,
            custoProdutoNoDecante,
            custoEmbalagem,
            custoTotal,
            precoVenda,
            lucroUnitario,
            margem,
            quantidadeDecantes,
            investimentoLote,
            receitaTotal,
            lucroTotal,
            margemLote
        );
    }

    private static Long idDoMelhor(List<AnaliseViabilidade> analises, Comparator<AnaliseViabilidade> criterio) {
        return analises.stream()
            .max(criterio)
            .map(AnaliseViabilidade::decanteId)
            .orElse(null);
    }

    /** ROI do lote: lucro total sobre o investimento. Usado só para ordenar a comparação. */
    private static BigDecimal retornoInvestimento(AnaliseViabilidade analise) {
        if (analise.investimentoLote().signum() == 0) {
            return BigDecimal.ZERO;
        }
        return analise.lucroTotal().divide(analise.investimentoLote(), ESCALA_PERCENTUAL + 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(ESCALA_PERCENTUAL);
        }
        return parte.multiply(CEM).divide(total, ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
    }

    private static BigDecimal dinheiro(BigDecimal valor) {
        return valor.setScale(ESCALA_DINHEIRO, RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static BigDecimal exigirPreenchido(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " é obrigatório para a análise");
        }
        return valor;
    }

    private static BigDecimal exigirPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero para a análise");
        }
        return valor;
    }

    private static void exigirMesmoProduto(Produto produto, Decante decante) {
        if (produto.getId() != null && decante.getProdutoId() != null
                && !produto.getId().equals(decante.getProdutoId())) {
            throw new IllegalArgumentException("O decante não pertence ao produto informado");
        }
    }
}
