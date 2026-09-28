package meu.negocio.com.br.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import meu.negocio.com.br.entity.Venda;
import meu.negocio.com.br.entity.Venda.TipoVenda;

/**
 * Repositório da entidade {@link Venda}.
 */
@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    Page<Venda> findByProdutoId(Long produtoId, Pageable pageable);

    Page<Venda> findByTipo(TipoVenda tipo, Pageable pageable);

    Page<Venda> findByProdutoIdAndTipo(Long produtoId, TipoVenda tipo, Pageable pageable);

    List<Venda> findByAtivoTrue();

    List<Venda> findByAtivoTrueAndDataVendaBetween(LocalDate de, LocalDate ate);

    List<Venda> findByProdutoIdAndTipoAndAtivoTrue(Long produtoId, TipoVenda tipo);

}
