package meu.negocio.com.br.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import meu.negocio.com.br.entity.Decante;


/***
 * Interface de repositório para a entidade Decante, fornecendo operações CRUD e consultas personalizadas.
 */
@Repository
public interface DecanteRepository extends JpaRepository<Decante, Long> {

    Page<Decante> findByProdutoId(Long produtoId, Pageable pageable);

}
