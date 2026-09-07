package meu.negocio.com.br.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import meu.negocio.com.br.entity.Marca;


/***
 * Interface de repositório para a entidade Marca, fornecendo operações CRUD e consultas personalizadas.
 */
@Repository
public interface MarcaRepository extends JpaRepository<Marca, Long> {


}
