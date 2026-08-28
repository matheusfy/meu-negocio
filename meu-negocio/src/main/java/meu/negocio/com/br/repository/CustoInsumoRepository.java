package meu.negocio.com.br.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import meu.negocio.com.br.entity.CustoInsumo;


/***
 * Interface de repositório para a entidade CustoInsumo, fornecendo operações CRUD e consultas personalizadas.
 */
@Repository
public interface CustoInsumoRepository extends JpaRepository<CustoInsumo, Long> {


}
