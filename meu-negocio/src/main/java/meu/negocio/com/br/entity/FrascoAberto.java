package meu.negocio.com.br.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Um frasco que a mãe abriu para decantar. Enquanto existe, esse frasco não pode mais ser
 * vendido cheio. Só controla o estoque de ml disponível para decante — o custo é resolvido
 * em cada venda (ver {@code planejamento_vendas_e_estoque.md}, §2.2 e §3.3).
 */
@Getter
@Setter
@Entity
@Table(
    name = "frascos_abertos",
    indexes = {
        @Index(name = "idx_frascos_abertos_produto_id", columnList = "produto_id"),
        @Index(name = "idx_frascos_abertos_status", columnList = "status"),
        @Index(name = "idx_frascos_abertos_user_id", columnList = "user_id")
    }
)
public class FrascoAberto extends Auditable {

    public enum StatusFrasco { ABERTO, ESGOTADO, DESCARTADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long produtoId;
    private LocalDate dataAbertura;
    private BigDecimal volumeInicialMl;
    private BigDecimal mlRestante;
    @Enumerated(EnumType.STRING)
    private StatusFrasco status = StatusFrasco.ABERTO;
    private String observacao;
    private int userId = 1;

    public FrascoAberto(Long produtoId, LocalDate dataAbertura, BigDecimal volumeInicialMl, String observacao) {
        this.produtoId = exigirProdutoValido(produtoId);
        this.dataAbertura = dataAbertura == null ? LocalDate.now() : dataAbertura;
        this.volumeInicialMl = exigirVolumeValido(volumeInicialMl);
        this.mlRestante = this.volumeInicialMl;
        this.status = StatusFrasco.ABERTO;
        this.observacao = observacao;
    }

    public FrascoAberto() {
        // Construtor padrão necessário para JPA
    }

    /** Consome ml deste frasco (venda de decante). Marca ESGOTADO quando não sobra nada. */
    public void consumir(BigDecimal ml) {
        if (ml == null || ml.signum() <= 0) {
            throw new IllegalArgumentException("Volume consumido deve ser maior que zero");
        }
        if (ml.compareTo(mlRestante) > 0) {
            throw new IllegalArgumentException("Frasco aberto não tem ml suficiente");
        }
        this.mlRestante = this.mlRestante.subtract(ml);
        if (this.mlRestante.signum() <= 0) {
            this.mlRestante = BigDecimal.ZERO.setScale(this.volumeInicialMl.scale());
            this.status = StatusFrasco.ESGOTADO;
        }
    }

    public void descartar() {
        this.status = StatusFrasco.DESCARTADO;
    }

    private static Long exigirProdutoValido(Long produtoId) {
        if (produtoId == null) {
            throw new IllegalArgumentException("Produto do frasco é obrigatório");
        }
        return produtoId;
    }

    private static BigDecimal exigirVolumeValido(BigDecimal volumeMl) {
        if (volumeMl == null || volumeMl.signum() <= 0) {
            throw new IllegalArgumentException("Volume do frasco deve ser maior que zero");
        }
        return volumeMl;
    }
}
