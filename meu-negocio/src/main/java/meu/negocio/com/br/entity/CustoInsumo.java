package meu.negocio.com.br.entity;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "custos_insumo",
    indexes = @Index(name = "idx_custos_insumo_user_id", columnList = "user_id")
)
public class CustoInsumo extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private BigDecimal custoUnitario;
    private String unidade;
    private int userId = 1;
    private boolean ativo;

    public CustoInsumo(String nome, BigDecimal custoUnitario, String unidade) {
        this.nome = exigirNomeValido(nome);
        this.custoUnitario = exigirCustoValido(custoUnitario);
        this.unidade = unidade;
        this.ativo = true;
    }

    public CustoInsumo() {
        // Construtor padrão necessário para JPA
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void atualizarCustoUnitario(BigDecimal novoCusto) {
        this.custoUnitario = exigirCustoValido(novoCusto);
    }

    private static String exigirNomeValido(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do insumo é obrigatório");
        }
        return nome;
    }

    private static BigDecimal exigirCustoValido(BigDecimal custo) {
        if (custo == null || custo.signum() < 0) {
            throw new IllegalArgumentException("Custo unitário não pode ser negativo");
        }
        return custo;
    }
}
