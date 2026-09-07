package meu.negocio.com.br.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import meu.negocio.com.br.entity.Marca;
import meu.negocio.com.br.exception.MarcaNaoEncontradaException;
import meu.negocio.com.br.repository.MarcaRepository;

@Service
public class MarcaService {

    private final MarcaRepository marcaRepository;

    public MarcaService(MarcaRepository marcaRepository) {
        this.marcaRepository = marcaRepository;
    }

    public Marca save(Marca marca) {
        marca.setId(null);
        return marcaRepository.save(marca);
    }

    public Marca findById(Long id) {
        return marcaRepository.findById(id)
            .orElseThrow(() -> new MarcaNaoEncontradaException(id));
    }

    public Marca update(Long id, Marca dadosAtualizados) {
        Marca marca = findById(id);
        marca.setNome(dadosAtualizados.getNome());
        marca.setPais(dadosAtualizados.getPais());
        if (dadosAtualizados.isAtivo()) {
            marca.ativar();
        } else {
            marca.desativar();
        }
        return marcaRepository.saveAndFlush(marca);
    }

    public void deleteById(Long id) {
        findById(id);
        marcaRepository.deleteById(id);
    }

    public Page<Marca> findAll(Pageable pageable) {
        return marcaRepository.findAll(pageable);
    }

}
