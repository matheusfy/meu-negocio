package meu.negocio.com.br.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import meu.negocio.com.br.entity.Produto;

public record ProdutoDTO(
    Long id,

    @NotBlank(message = "Nome do produto é obrigatório")
    String nome,

    String marca,
    String categoria,
    String sku,
    Long fornecedorPrincipalId,

    @PositiveOrZero(message = "Preço de venda não pode ser negativo")
    BigDecimal precoVenda,

    @PositiveOrZero(message = "Preço de custo não pode ser negativo")
    BigDecimal precoCusto,

    @PositiveOrZero(message = "Volume não pode ser negativo")
    BigDecimal volumeMl,

    @PositiveOrZero(message = "Estoque atual não pode ser negativo")
    int estoqueAtual,

    @PositiveOrZero(message = "Estoque mínimo não pode ser negativo")
    int estoqueMinimo,

    boolean ativo,

    // Somente leitura: preenchidos pela auditoria do JPA, ignorados em create/update.
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm,
    Long criadoPor,
    Long atualizadoPor
) {

    public ProdutoDTO(Produto produto) {
        this(
            produto.getId(),
            produto.getNome(),
            produto.getMarca(),
            produto.getCategoria(),
            produto.getSku(),
            produto.getFornecedorPrincipalId(),
            produto.getPrecoVenda(),
            produto.getPrecoCusto(),
            produto.getVolumeMl(),
            produto.getEstoqueAtual(),
            produto.getEstoqueMinimo(),
            produto.isAtivo(),
            produto.getCriadoEm(),
            produto.getAtualizadoEm(),
            produto.getCriadoPor(),
            produto.getAtualizadoPor()
        );
    }

    public Produto toEntity() {
        Produto produto = new Produto();
        produto.setId(this.id);
        produto.setNome(this.nome);
        produto.setMarca(this.marca);
        produto.setCategoria(this.categoria);
        produto.setSku(this.sku);
        produto.setFornecedorPrincipalId(this.fornecedorPrincipalId);
        produto.setPrecoVenda(this.precoVenda);
        produto.setPrecoCusto(this.precoCusto);
        produto.setVolumeMl(this.volumeMl);
        produto.setEstoqueAtual(this.estoqueAtual);
        produto.setEstoqueMinimo(this.estoqueMinimo);
        produto.setAtivo(this.ativo);
        return produto;
    }
}
