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
 * Uma venda realizada. Guarda os valores <b>congelados</b> no momento da venda
 * ({@code custoUnitario}, {@code receitaTotal}, {@code lucroTotal}) — lançar um lote novo
 * depois não muda o lucro desta venda (ver {@code planejamento_vendas_e_estoque.md}, §2.3).
 */
@Getter
@Setter
@Entity
@Table(
    name = "vendas",
    indexes = {
        @Index(name = "idx_vendas_produto_id", columnList = "produto_id"),
        @Index(name = "idx_vendas_user_id", columnList = "user_id"),
        @Index(name = "idx_vendas_data_venda", columnList = "data_venda"),
        @Index(name = "idx_vendas_tipo", columnList = "tipo")
    }
)
public class Venda extends Auditable {

    public enum TipoVenda { VIDRO_CHEIO, DECANTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long produtoId;
    private LocalDate dataVenda;
    @Enumerated(EnumType.STRING)
    private TipoVenda tipo;
    private Long decanteId;
    private Long frascoAbertoId;
    private int quantidade;
    private BigDecimal volumeUnitarioMl;
    private BigDecimal precoUnitario;
    private BigDecimal custoUnitario;
    private BigDecimal receitaTotal;
    private BigDecimal lucroTotal;
    private String formaPagamento;
    private String observacao;
    private int userId = 1;
    private boolean ativo;

    public Venda(Long produtoId, LocalDate dataVenda, TipoVenda tipo, int quantidade, BigDecimal precoUnitario,
            String formaPagamento, String observacao) {
        this.produtoId = exigirProdutoValido(produtoId);
        this.dataVenda = dataVenda == null ? LocalDate.now() : dataVenda;
        this.tipo = exigirTipoValido(tipo);
        this.quantidade = exigirQuantidadeValida(quantidade);
        this.precoUnitario = exigirValorNaoNegativo(precoUnitario, "Preço unitário");
        this.formaPagamento = formaPagamento;
        this.observacao = observacao;
        this.ativo = true;
    }

    public Venda() {
        // Construtor padrão necessário para JPA
    }

    public void ativar() {
        this.ativo = true;
    }

    public void cancelar() {
        this.ativo = false;
    }

    private static Long exigirProdutoValido(Long produtoId) {
        if (produtoId == null) {
            throw new IllegalArgumentException("Produto da venda é obrigatório");
        }
        return produtoId;
    }

    private static TipoVenda exigirTipoValido(TipoVenda tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo da venda é obrigatório");
        }
        return tipo;
    }

    private static int exigirQuantidadeValida(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade da venda deve ser maior que zero");
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
