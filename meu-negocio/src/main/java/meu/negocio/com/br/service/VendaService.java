package meu.negocio.com.br.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import meu.negocio.com.br.dto.EstoqueProduto;
import meu.negocio.com.br.entity.Produto;
import meu.negocio.com.br.entity.Venda;
import meu.negocio.com.br.entity.Venda.TipoVenda;
import meu.negocio.com.br.exception.VendaNaoEncontradaException;
import meu.negocio.com.br.repository.VendaRepository;

/**
 * Registro de vendas. Na Fase 2 só trata {@code VIDRO_CHEIO}: valida o estoque de frascos
 * lacrados e <b>congela</b> custo/receita/lucro no momento da venda
 * (ver {@code planejamento_vendas_e_estoque.md}, §3.3). A baixa de estoque é automática —
 * {@code frascosLacrados} é derivado, então registrar/excluir a venda já ajusta o saldo.
 */
@Service
public class VendaService {

    private static final int ESCALA_DINHEIRO = 2;

    private final VendaRepository vendaRepository;
    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;

    public VendaService(VendaRepository vendaRepository, ProdutoService produtoService,
            EstoqueService estoqueService) {
        this.vendaRepository = vendaRepository;
        this.produtoService = produtoService;
        this.estoqueService = estoqueService;
    }

    public Venda registrar(Venda venda) {
        Produto produto = produtoService.findById(venda.getProdutoId());
        venda.setId(null);
        if (venda.getDataVenda() == null) {
            venda.setDataVenda(LocalDate.now());
        }
        exigirVidroCheio(venda.getTipo());
        venda.setTipo(TipoVenda.VIDRO_CHEIO);
        venda.setDecanteId(null);
        venda.setFrascoAbertoId(null);

        EstoqueProduto estoque = estoqueService.calcular(produto.getId());
        exigirEstoqueSuficiente(estoque.frascosLacrados(), venda.getQuantidade());
        BigDecimal custoFrasco = exigirCustoConhecido(estoque.custoMedioFrasco());

        aplicarSnapshot(venda, produto.getVolumeMl(), custoFrasco);
        return vendaRepository.save(venda);
    }

    public Venda findById(Long id) {
        return vendaRepository.findById(id)
            .orElseThrow(() -> new VendaNaoEncontradaException(id));
    }

    public Venda update(Long id, Venda dadosAtualizados) {
        Venda venda = findById(id);
        exigirVidroCheio(dadosAtualizados.getTipo());
        Produto produto = produtoService.findById(dadosAtualizados.getProdutoId());

        EstoqueProduto estoque = estoqueService.calcular(dadosAtualizados.getProdutoId());
        int jaConsumidoPorEstaVenda =
            Objects.equals(venda.getProdutoId(), dadosAtualizados.getProdutoId())
                && venda.getTipo() == TipoVenda.VIDRO_CHEIO && venda.isAtivo()
            ? venda.getQuantidade() : 0;
        exigirEstoqueSuficiente(estoque.frascosLacrados() + jaConsumidoPorEstaVenda, dadosAtualizados.getQuantidade());
        BigDecimal custoFrasco = exigirCustoConhecido(estoque.custoMedioFrasco());

        venda.setProdutoId(dadosAtualizados.getProdutoId());
        venda.setDataVenda(dadosAtualizados.getDataVenda() == null ? LocalDate.now() : dadosAtualizados.getDataVenda());
        venda.setTipo(TipoVenda.VIDRO_CHEIO);
        venda.setDecanteId(null);
        venda.setFrascoAbertoId(null);
        venda.setQuantidade(dadosAtualizados.getQuantidade());
        venda.setPrecoUnitario(dadosAtualizados.getPrecoUnitario());
        venda.setFormaPagamento(dadosAtualizados.getFormaPagamento());
        venda.setObservacao(dadosAtualizados.getObservacao());
        if (dadosAtualizados.isAtivo()) {
            venda.ativar();
        } else {
            venda.cancelar();
        }
        aplicarSnapshot(venda, produto.getVolumeMl(), custoFrasco);
        return vendaRepository.saveAndFlush(venda);
    }

    public void deleteById(Long id) {
        findById(id);
        vendaRepository.deleteById(id);
    }

    public Page<Venda> findAll(Long produtoId, String tipo, Pageable pageable) {
        TipoVenda t = (tipo != null && !tipo.isBlank())
            ? TipoVenda.valueOf(tipo.trim().toUpperCase())
            : null;
        if (produtoId != null && t != null) {
            return vendaRepository.findByProdutoIdAndTipo(produtoId, t, pageable);
        }
        if (produtoId != null) {
            return vendaRepository.findByProdutoId(produtoId, pageable);
        }
        if (t != null) {
            return vendaRepository.findByTipo(t, pageable);
        }
        return vendaRepository.findAll(pageable);
    }

    /**
     * Congela volume, custo, receita e lucro da venda. Puro — não toca banco nem estoque;
     * é o ponto testável do cálculo.
     */
    public void aplicarSnapshot(Venda venda, BigDecimal volumeUnitarioMl, BigDecimal custoUnitario) {
        BigDecimal quantidade = BigDecimal.valueOf(venda.getQuantidade());
        BigDecimal preco = dinheiro(nvl(venda.getPrecoUnitario()));
        BigDecimal custo = dinheiro(nvl(custoUnitario));
        venda.setVolumeUnitarioMl(volumeUnitarioMl);
        venda.setPrecoUnitario(preco);
        venda.setCustoUnitario(custo);
        venda.setReceitaTotal(dinheiro(preco.multiply(quantidade)));
        venda.setLucroTotal(dinheiro(preco.subtract(custo).multiply(quantidade)));
    }

    private static void exigirVidroCheio(TipoVenda tipo) {
        if (tipo == TipoVenda.DECANTE) {
            throw new IllegalArgumentException("Venda de decante ainda não está disponível (chega na Fase 3)");
        }
    }

    private static void exigirEstoqueSuficiente(int lacradosDisponiveis, int quantidade) {
        if (lacradosDisponiveis < quantidade) {
            throw new IllegalArgumentException(
                "Estoque insuficiente: " + Math.max(lacradosDisponiveis, 0)
                + " frasco(s) lacrado(s) para vender " + quantidade);
        }
    }

    private static BigDecimal exigirCustoConhecido(BigDecimal custoFrasco) {
        if (custoFrasco == null) {
            throw new IllegalArgumentException(
                "Sem custo do frasco: cadastre uma compra em Estoque ou informe o preço de custo do perfume");
        }
        return custoFrasco;
    }

    private static BigDecimal dinheiro(BigDecimal valor) {
        return valor.setScale(ESCALA_DINHEIRO, RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
