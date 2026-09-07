package meu.negocio.com.br.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import meu.negocio.com.br.dto.EstoqueProduto;
import meu.negocio.com.br.entity.FrascoAberto;
import meu.negocio.com.br.entity.FrascoAberto.StatusFrasco;
import meu.negocio.com.br.entity.LoteCompra;
import meu.negocio.com.br.entity.Produto;
import meu.negocio.com.br.repository.FrascoAbertoRepository;
import meu.negocio.com.br.repository.LoteCompraRepository;
import meu.negocio.com.br.repository.ProdutoRepository;

/**
 * Consolida o estoque de um perfume a partir das compras ({@link LoteCompra}) e dos
 * frascos abertos ({@link FrascoAberto}). Também mantém {@code produto.precoCusto} como
 * cache do custo médio ponderado (ver {@code planejamento_vendas_e_estoque.md}, §3).
 */
@Service
public class EstoqueService {

    /** Casas decimais para valores monetários (R$). */
    private static final int ESCALA_DINHEIRO = 2;
    /** Casas decimais para o custo por ml (mais preciso, evita acumular erro). */
    private static final int ESCALA_CUSTO_ML = 4;

    private final ProdutoService produtoService;
    private final ProdutoRepository produtoRepository;
    private final LoteCompraRepository loteCompraRepository;
    private final FrascoAbertoRepository frascoAbertoRepository;

    public EstoqueService(ProdutoService produtoService, ProdutoRepository produtoRepository,
            LoteCompraRepository loteCompraRepository, FrascoAbertoRepository frascoAbertoRepository) {
        this.produtoService = produtoService;
        this.produtoRepository = produtoRepository;
        this.loteCompraRepository = loteCompraRepository;
        this.frascoAbertoRepository = frascoAbertoRepository;
    }

    /** Carrega os dados do produto e devolve a foto do estoque. */
    public EstoqueProduto calcular(Long produtoId) {
        Produto produto = produtoService.findById(produtoId);
        List<LoteCompra> lotes = loteCompraRepository.findByProdutoIdAndAtivoTrueOrderByDataCompraAsc(produtoId);
        List<FrascoAberto> frascos = frascoAbertoRepository.findByProdutoIdOrderByDataAberturaAsc(produtoId);
        // Fase 2 liga o nº de frascos vendidos cheios; até lá é zero.
        return calcular(produto, lotes, frascos, 0L);
    }

    /**
     * Cálculo puro do estoque. Testável sem banco: recebe as entidades já carregadas.
     */
    public EstoqueProduto calcular(Produto produto, List<LoteCompra> lotes, List<FrascoAberto> frascosAbertos,
            long frascosVendidosCheios) {
        int frascosComprados = lotes.stream().mapToInt(LoteCompra::getQuantidadeFrascos).sum();
        int frascosAbertosTotais = frascosAbertos.size();
        long abertosAtivos = frascosAbertos.stream()
            .filter(f -> f.getStatus() == StatusFrasco.ABERTO)
            .count();
        BigDecimal mlNosAbertos = frascosAbertos.stream()
            .filter(f -> f.getStatus() == StatusFrasco.ABERTO)
            .map(FrascoAberto::getMlRestante)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(ESCALA_DINHEIRO, RoundingMode.HALF_UP);

        int frascosLacrados = (int) (frascosComprados - frascosVendidosCheios - frascosAbertosTotais);

        boolean temCompras = !lotes.isEmpty();
        BigDecimal custoMedioFrasco = custoMedioFrasco(lotes);
        if (custoMedioFrasco == null) {
            custoMedioFrasco = produto.getPrecoCusto();
        }

        BigDecimal volumeMl = produto.getVolumeMl();
        BigDecimal custoMedioMl = null;
        if (custoMedioFrasco != null && volumeMl != null && volumeMl.signum() > 0) {
            custoMedioMl = custoMedioFrasco.divide(volumeMl, ESCALA_CUSTO_ML, RoundingMode.HALF_UP);
        }

        BigDecimal valorEstoque = BigDecimal.ZERO.setScale(ESCALA_DINHEIRO);
        if (custoMedioFrasco != null) {
            valorEstoque = custoMedioFrasco.multiply(BigDecimal.valueOf(Math.max(frascosLacrados, 0)));
            if (custoMedioMl != null) {
                valorEstoque = valorEstoque.add(custoMedioMl.multiply(mlNosAbertos));
            }
            valorEstoque = valorEstoque.setScale(ESCALA_DINHEIRO, RoundingMode.HALF_UP);
        }

        int estoqueMinimo = produto.getEstoqueMinimo();
        boolean abaixoDoMinimo = estoqueMinimo > 0 && frascosLacrados <= estoqueMinimo;

        return new EstoqueProduto(
            produto.getId(),
            frascosComprados,
            (int) frascosVendidosCheios,
            (int) abertosAtivos,
            frascosLacrados,
            mlNosAbertos,
            custoMedioFrasco,
            custoMedioMl,
            valorEstoque,
            estoqueMinimo,
            abaixoDoMinimo,
            frascosLacrados > 0,
            temCompras
        );
    }

    /**
     * Média ponderada do custo do frasco entre todos os lotes ativos.
     * Devolve {@code null} quando não há lote (aí o custo cai no valor manual do produto).
     */
    public BigDecimal custoMedioFrasco(List<LoteCompra> lotes) {
        if (lotes.isEmpty()) {
            return null;
        }
        BigDecimal somaValor = BigDecimal.ZERO;
        long somaQuantidade = 0;
        for (LoteCompra lote : lotes) {
            int quantidade = lote.getQuantidadeFrascos();
            somaValor = somaValor.add(lote.custoEfetivoFrasco().multiply(BigDecimal.valueOf(quantidade)));
            somaQuantidade += quantidade;
        }
        if (somaQuantidade == 0) {
            return null;
        }
        return somaValor.divide(BigDecimal.valueOf(somaQuantidade), ESCALA_DINHEIRO, RoundingMode.HALF_UP);
    }

    /**
     * Recalcula {@code produto.precoCusto} a partir das compras. Sem nenhum lote lançado,
     * mantém o valor que a mãe digitou (não sobrescreve).
     */
    public void recalcularCustoNoProduto(Long produtoId) {
        List<LoteCompra> lotes = loteCompraRepository.findByProdutoIdAndAtivoTrueOrderByDataCompraAsc(produtoId);
        BigDecimal medio = custoMedioFrasco(lotes);
        if (medio == null) {
            return;
        }
        Produto produto = produtoService.findById(produtoId);
        produto.atualizarPrecoCusto(medio);
        produtoRepository.save(produto);
    }
}
