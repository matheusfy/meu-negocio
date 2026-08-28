package meu.negocio.com.br.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import meu.negocio.com.br.entity.Produto;
import meu.negocio.com.br.exception.ProdutoNaoEncontradoException;
import meu.negocio.com.br.repository.ProdutoRepository;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public Produto save(Produto produto) {
        produto.setId(null);
        return produtoRepository.save(produto);
    }

    public Produto findById(Long id) {
        return produtoRepository.findById(id)
            .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    public Produto update(Long id, Produto dadosAtualizados) {
        Produto produto = findById(id);
        produto.setNome(dadosAtualizados.getNome());
        produto.setMarca(dadosAtualizados.getMarca());
        produto.setCategoria(dadosAtualizados.getCategoria());
        produto.setSku(dadosAtualizados.getSku());
        produto.setFornecedorPrincipalId(dadosAtualizados.getFornecedorPrincipalId());
        produto.atualizarPrecoVenda(dadosAtualizados.getPrecoVenda());
        produto.atualizarPrecoCusto(dadosAtualizados.getPrecoCusto());
        produto.setVolumeMl(dadosAtualizados.getVolumeMl());
        produto.setEstoqueAtual(dadosAtualizados.getEstoqueAtual());
        produto.setEstoqueMinimo(dadosAtualizados.getEstoqueMinimo());
        if (dadosAtualizados.isAtivo()) {
            produto.ativar();
        } else {
            produto.desativar();
        }
        return produtoRepository.saveAndFlush(produto);
    }

    public void deleteById(Long id) {
        findById(id);
        produtoRepository.deleteById(id);
    }

    public Page<Produto> findAll(Pageable pageable) {
        return produtoRepository.findAll(pageable);
    }

}
