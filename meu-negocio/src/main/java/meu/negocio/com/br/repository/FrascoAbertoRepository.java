package meu.negocio.com.br.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import meu.negocio.com.br.entity.FrascoAberto;
import meu.negocio.com.br.entity.FrascoAberto.StatusFrasco;

/**
 * Repositório da entidade {@link FrascoAberto} — frascos abertos para decantar.
 */
@Repository
public interface FrascoAbertoRepository extends JpaRepository<FrascoAberto, Long> {

    Page<FrascoAberto> findByProdutoId(Long produtoId, Pageable pageable);

    Page<FrascoAberto> findByProdutoIdAndStatus(Long produtoId, StatusFrasco status, Pageable pageable);

    List<FrascoAberto> findByProdutoIdOrderByDataAberturaAsc(Long produtoId);

}
