package meu.negocio.com.br.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import meu.negocio.com.br.entity.CustoInsumo;
import meu.negocio.com.br.exception.CustoInsumoNaoEncontradoException;
import meu.negocio.com.br.repository.CustoInsumoRepository;

@Service
public class CustoInsumoService {

    private final CustoInsumoRepository custoInsumoRepository;

    public CustoInsumoService(CustoInsumoRepository custoInsumoRepository) {
        this.custoInsumoRepository = custoInsumoRepository;
    }

    public CustoInsumo save(CustoInsumo custoInsumo) {
        custoInsumo.setId(null);
        return custoInsumoRepository.save(custoInsumo);
    }

    public CustoInsumo findById(Long id) {
        return custoInsumoRepository.findById(id)
            .orElseThrow(() -> new CustoInsumoNaoEncontradoException(id));
    }

    public CustoInsumo update(Long id, CustoInsumo dadosAtualizados) {
        CustoInsumo custoInsumo = findById(id);
        custoInsumo.setNome(dadosAtualizados.getNome());
        custoInsumo.atualizarCustoUnitario(dadosAtualizados.getCustoUnitario());
        custoInsumo.setUnidade(dadosAtualizados.getUnidade());
        if (dadosAtualizados.isAtivo()) {
            custoInsumo.ativar();
        } else {
            custoInsumo.desativar();
        }
        return custoInsumoRepository.saveAndFlush(custoInsumo);
    }

    public void deleteById(Long id) {
        findById(id);
        custoInsumoRepository.deleteById(id);
    }

    public Page<CustoInsumo> findAll(Pageable pageable) {
        return custoInsumoRepository.findAll(pageable);
    }

}
