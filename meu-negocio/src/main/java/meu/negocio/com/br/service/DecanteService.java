package meu.negocio.com.br.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import meu.negocio.com.br.entity.Decante;
import meu.negocio.com.br.exception.DecanteNaoEncontradoException;
import meu.negocio.com.br.repository.DecanteRepository;

@Service
public class DecanteService {

    private final DecanteRepository decanteRepository;
    private final ProdutoService produtoService;

    public DecanteService(DecanteRepository decanteRepository, ProdutoService produtoService) {
        this.decanteRepository = decanteRepository;
        this.produtoService = produtoService;
    }

    public Decante save(Decante decante) {
        produtoService.findById(decante.getProdutoId());
        decante.setId(null);
        return decanteRepository.save(decante);
    }

    public Decante findById(Long id) {
        return decanteRepository.findById(id)
            .orElseThrow(() -> new DecanteNaoEncontradoException(id));
    }

    public Decante update(Long id, Decante dadosAtualizados) {
        Decante decante = findById(id);
        produtoService.findById(dadosAtualizados.getProdutoId());
        decante.setProdutoId(dadosAtualizados.getProdutoId());
        decante.setVolumeMl(dadosAtualizados.getVolumeMl());
        decante.atualizarPrecoVenda(dadosAtualizados.getPrecoVenda());
        decante.setCustoEmbalagem(dadosAtualizados.getCustoEmbalagem());
        decante.setCustoSeringa(dadosAtualizados.getCustoSeringa());
        decante.setCustoEtiqueta(dadosAtualizados.getCustoEtiqueta());
        if (dadosAtualizados.isAtivo()) {
            decante.ativar();
        } else {
            decante.desativar();
        }
        return decanteRepository.saveAndFlush(decante);
    }

    public void deleteById(Long id) {
        findById(id);
        decanteRepository.deleteById(id);
    }

    public Page<Decante> findAll(Long produtoId, Pageable pageable) {
        if (produtoId != null) {
            return decanteRepository.findByProdutoId(produtoId, pageable);
        }
        return decanteRepository.findAll(pageable);
    }

}
