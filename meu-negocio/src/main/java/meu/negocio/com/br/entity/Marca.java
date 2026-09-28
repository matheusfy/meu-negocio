package meu.negocio.com.br.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Marca de perfume (ex: Chanel, Dior). Vira cadastro próprio para agrupar várias
 * linhas da mesma marca e, futuramente, cortar análises por marca.
 */
@Getter
@Setter
@Entity
@Table(
    name = "marcas",
    uniqueConstraints = @UniqueConstraint(name = "uk_marcas_nome_user_id", columnNames = {"nome", "user_id"}),
    indexes = @Index(name = "idx_marcas_user_id", columnList = "user_id")
)
public class Marca extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    /** País de origem da marca; opcional, útil para segmentar o mercado depois. */
    private String pais;
    private int userId = 1;
    private boolean ativo;

    public Marca(String nome, String pais) {
        this.nome = exigirNomeValido(nome);
        this.pais = pais;
        this.ativo = true;
    }

    public Marca() {
        // Construtor padrão necessário para JPA
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    private static String exigirNomeValido(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da marca é obrigatório");
        }
        return nome;
    }
}
