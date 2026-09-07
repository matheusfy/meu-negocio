package meu.negocio.com.br.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import meu.negocio.com.br.entity.LoteCompra;

/**
 * Repositório da entidade {@link LoteCompra} — compras de frascos que alimentam o estoque.
 */
@Repository
public interface LoteCompraRepository extends JpaRepository<LoteCompra, Long> {

    Page<LoteCompra> findByProdutoId(Long produtoId, Pageable pageable);

    List<LoteCompra> findByProdutoIdAndAtivoTrueOrderByDataCompraAsc(Long produtoId);

}
