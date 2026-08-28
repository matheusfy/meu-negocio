package meu.negocio.com.br.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import meu.negocio.com.br.entity.Produto;


/***
 * Interface de repositório para a entidade Produto, fornecendo operações CRUD e consultas personalizadas.
 */
@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {


}
