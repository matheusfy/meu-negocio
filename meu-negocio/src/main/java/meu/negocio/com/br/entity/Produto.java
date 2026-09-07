package meu.negocio.com.br.entity;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "produtos",
    uniqueConstraints = @UniqueConstraint(name = "uk_produtos_sku_user_id", columnNames = {"sku", "user_id"}),
    indexes = @Index(name = "idx_produtos_user_id", columnList = "user_id")
)
public class Produto extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private Long marcaId;
    private String categoria;
    private String sku;
    private Long fornecedorPrincipalId;
    private BigDecimal precoVenda;
    private BigDecimal precoCusto;
    private BigDecimal volumeMl;
    private int estoqueAtual;
    private int estoqueMinimo;
    private int userId = 1;
    private boolean ativo;

    public Produto(String nome, Long marcaId, String categoria, String sku, Long fornecedorPrincipalId, BigDecimal precoVenda, BigDecimal precoCusto, BigDecimal volumeMl, int estoqueAtual, int estoqueMinimo) {
        this.nome = exigirNomeValido(nome);
        this.marcaId = marcaId;
        this.categoria = categoria;
        this.sku = sku;
        this.fornecedorPrincipalId = fornecedorPrincipalId;
        this.precoVenda = exigirPrecoValido(precoVenda, "Preço de venda");
        this.precoCusto = exigirPrecoValido(precoCusto, "Preço de custo");
        this.volumeMl = volumeMl;
        this.estoqueMinimo = exigirNaoNegativo(estoqueMinimo, "Estoque mínimo");
        this.estoqueAtual = exigirNaoNegativo(estoqueAtual, "Estoque atual");
        this.ativo = true;
    }

    public Produto() {
        // Construtor padrão necessário para JPA
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void atualizarPrecoVenda(BigDecimal novoPreco) {
        this.precoVenda = exigirPrecoValido(novoPreco, "Preço de venda");
    }

    public void atualizarPrecoCusto(BigDecimal novoPreco) {
        this.precoCusto = exigirPrecoValido(novoPreco, "Preço de custo");
    }

    public boolean estoqueBaixo() {
        return estoqueAtual <= estoqueMinimo;
    }

    private static String exigirNomeValido(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do produto é obrigatório");
        }
        return nome;
    }

    private static BigDecimal exigirPrecoValido(BigDecimal preco, String campo) {
        if (preco == null || preco.signum() < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo");
        }
        return preco;
    }

    private static int exigirNaoNegativo(int valor, String campo) {
        if (valor < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo");
        }
        return valor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public Long getMarcaId() {
        return marcaId;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getSku() {
        return sku;
    }

    public Long getFornecedorPrincipalId() {
        return fornecedorPrincipalId;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public BigDecimal getPrecoCusto() {
        return precoCusto;
    }

    public int getEstoqueAtual() {
        return estoqueAtual;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
