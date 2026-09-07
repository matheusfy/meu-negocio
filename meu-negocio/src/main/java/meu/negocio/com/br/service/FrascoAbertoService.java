package meu.negocio.com.br.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import meu.negocio.com.br.dto.EstoqueProduto;
import meu.negocio.com.br.entity.FrascoAberto;
import meu.negocio.com.br.entity.FrascoAberto.StatusFrasco;
import meu.negocio.com.br.entity.Produto;
import meu.negocio.com.br.exception.FrascoAbertoNaoEncontradoException;
import meu.negocio.com.br.repository.FrascoAbertoRepository;

/**
 * Abertura e baixa de frascos para decante. Abrir um frasco exige ao menos um frasco
 * lacrado em estoque; a "exclusão" apenas marca o frasco como DESCARTADO (o frasco já
 * saiu do estoque de lacrados e não volta).
 */
@Service
public class FrascoAbertoService {

    private final FrascoAbertoRepository frascoAbertoRepository;
    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;

    public FrascoAbertoService(FrascoAbertoRepository frascoAbertoRepository, ProdutoService produtoService,
            EstoqueService estoqueService) {
        this.frascoAbertoRepository = frascoAbertoRepository;
        this.produtoService = produtoService;
        this.estoqueService = estoqueService;
    }

    public FrascoAberto abrir(Long produtoId, LocalDate dataAbertura, String observacao) {
        Produto produto = produtoService.findById(produtoId);
        if (produto.getVolumeMl() == null || produto.getVolumeMl().signum() <= 0) {
            throw new IllegalArgumentException("Informe o volume do frasco do perfume antes de abrir um frasco");
        }
        EstoqueProduto estoque = estoqueService.calcular(produtoId);
        if (estoque.frascosLacrados() < 1) {
            throw new IllegalArgumentException("Não há frasco lacrado em estoque para abrir");
        }
        FrascoAberto frasco = new FrascoAberto(produtoId, dataAbertura, produto.getVolumeMl(), observacao);
        return frascoAbertoRepository.save(frasco);
    }

    public FrascoAberto findById(Long id) {
        return frascoAbertoRepository.findById(id)
            .orElseThrow(() -> new FrascoAbertoNaoEncontradoException(id));
    }

    public FrascoAberto descartar(Long id) {
        FrascoAberto frasco = findById(id);
        frasco.descartar();
        return frascoAbertoRepository.saveAndFlush(frasco);
    }

    public Page<FrascoAberto> findAll(Long produtoId, String status, Pageable pageable) {
        if (produtoId != null && status != null && !status.isBlank()) {
            return frascoAbertoRepository.findByProdutoIdAndStatus(
                produtoId, StatusFrasco.valueOf(status.trim().toUpperCase()), pageable);
        }
        if (produtoId != null) {
            return frascoAbertoRepository.findByProdutoId(produtoId, pageable);
        }
        return frascoAbertoRepository.findAll(pageable);
    }

}
