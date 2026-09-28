package meu.negocio.com.br.service;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import meu.negocio.com.br.entity.LoteCompra;
import meu.negocio.com.br.exception.LoteCompraNaoEncontradoException;
import meu.negocio.com.br.repository.LoteCompraRepository;

/**
 * CRUD dos lotes de compra. Toda alteração dispara o recálculo do custo médio
 * do perfume no {@link EstoqueService}.
 */
@Service
public class LoteCompraService {

    private final LoteCompraRepository loteCompraRepository;
    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;

    public LoteCompraService(LoteCompraRepository loteCompraRepository, ProdutoService produtoService,
            EstoqueService estoqueService) {
        this.loteCompraRepository = loteCompraRepository;
        this.produtoService = produtoService;
        this.estoqueService = estoqueService;
    }

    public LoteCompra save(LoteCompra lote) {
        produtoService.findById(lote.getProdutoId());
        lote.setId(null);
        if (lote.getDataCompra() == null) {
            lote.setDataCompra(LocalDate.now());
        }
        LoteCompra salvo = loteCompraRepository.save(lote);
        estoqueService.recalcularCustoNoProduto(salvo.getProdutoId());
        return salvo;
    }

    public LoteCompra findById(Long id) {
        return loteCompraRepository.findById(id)
            .orElseThrow(() -> new LoteCompraNaoEncontradoException(id));
    }

    public LoteCompra update(Long id, LoteCompra dadosAtualizados) {
        LoteCompra lote = findById(id);
        Long produtoAnterior = lote.getProdutoId();
        produtoService.findById(dadosAtualizados.getProdutoId());

        lote.setProdutoId(dadosAtualizados.getProdutoId());
        lote.setDataCompra(dadosAtualizados.getDataCompra() == null ? LocalDate.now() : dadosAtualizados.getDataCompra());
        lote.setQuantidadeFrascos(dadosAtualizados.getQuantidadeFrascos());
        lote.setPrecoUnitario(dadosAtualizados.getPrecoUnitario());
        lote.setCustoAdicional(dadosAtualizados.getCustoAdicional());
        lote.setFornecedor(dadosAtualizados.getFornecedor());
        lote.setObservacao(dadosAtualizados.getObservacao());
        if (dadosAtualizados.isAtivo()) {
            lote.ativar();
        } else {
            lote.desativar();
        }
        LoteCompra salvo = loteCompraRepository.saveAndFlush(lote);

        estoqueService.recalcularCustoNoProduto(salvo.getProdutoId());
        if (!Objects.equals(produtoAnterior, salvo.getProdutoId())) {
            estoqueService.recalcularCustoNoProduto(produtoAnterior);
        }
        return salvo;
    }

    public void deleteById(Long id) {
        LoteCompra lote = findById(id);
        Long produtoId = lote.getProdutoId();
        loteCompraRepository.deleteById(id);
        estoqueService.recalcularCustoNoProduto(produtoId);
    }

    public Page<LoteCompra> findAll(Long produtoId, Pageable pageable) {
        if (produtoId != null) {
            return loteCompraRepository.findByProdutoId(produtoId, pageable);
        }
        return loteCompraRepository.findAll(pageable);
    }

}
