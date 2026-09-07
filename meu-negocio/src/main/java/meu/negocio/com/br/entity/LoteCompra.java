package meu.negocio.com.br.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Uma entrada de estoque: "comprei N frascos do perfume X no dia D a R$ P cada".
 * A média ponderada desses lotes é o custo médio do frasco usado para calcular lucro
 * (ver {@code planejamento_vendas_e_estoque.md}, §3.1).
 */
@Getter
@Setter
@Entity
@Table(
    name = "lotes_compra",
    indexes = {
        @Index(name = "idx_lotes_compra_produto_id", columnList = "produto_id"),
        @Index(name = "idx_lotes_compra_user_id", columnList = "user_id"),
        @Index(name = "idx_lotes_compra_data_compra", columnList = "data_compra")
    }
)
public class LoteCompra extends Auditable {

    /** Casas decimais do rateio do custo adicional por frasco (mais preciso que dinheiro). */
    private static final int ESCALA_RATEIO = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long produtoId;
    private LocalDate dataCompra;
    private int quantidadeFrascos;
    private BigDecimal precoUnitario;
    /** Frete / taxas do pedido inteiro, rateado pelos frascos deste lote. */
    private BigDecimal custoAdicional = BigDecimal.ZERO;
    private String fornecedor;
    private String observacao;
    private int userId = 1;
    private boolean ativo;

    public LoteCompra(Long produtoId, LocalDate dataCompra, int quantidadeFrascos,
            BigDecimal precoUnitario, BigDecimal custoAdicional, String fornecedor, String observacao) {
        this.produtoId = exigirProdutoValido(produtoId);
        this.dataCompra = dataCompra == null ? LocalDate.now() : dataCompra;
        this.quantidadeFrascos = exigirQuantidadeValida(quantidadeFrascos);
        this.precoUnitario = exigirValorNaoNegativo(precoUnitario, "Preço unitário");
        this.custoAdicional = custoAdicional == null
            ? BigDecimal.ZERO
            : exigirValorNaoNegativo(custoAdicional, "Custo adicional");
        this.fornecedor = fornecedor;
        this.observacao = observacao;
        this.ativo = true;
    }

    public LoteCompra() {
        // Construtor padrão necessário para JPA
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    /** Preço pago por frasco já com o custo adicional do pedido rateado. */
    public BigDecimal custoEfetivoFrasco() {
        BigDecimal adicional = custoAdicional == null ? BigDecimal.ZERO : custoAdicional;
        if (quantidadeFrascos <= 0) {
            return precoUnitario;
        }
        BigDecimal rateio = adicional.divide(BigDecimal.valueOf(quantidadeFrascos), ESCALA_RATEIO, RoundingMode.HALF_UP);
        return precoUnitario.add(rateio);
    }

    private static Long exigirProdutoValido(Long produtoId) {
        if (produtoId == null) {
            throw new IllegalArgumentException("Produto do lote é obrigatório");
        }
        return produtoId;
    }

    private static int exigirQuantidadeValida(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade de frascos deve ser maior que zero");
        }
        return quantidade;
    }

    private static BigDecimal exigirValorNaoNegativo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo");
        }
        return valor;
    }
}
