package meu.negocio.com.br.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import meu.negocio.com.br.entity.LoteCompra;

public record LoteCompraDTO(
    Long id,

    @NotNull(message = "Produto do lote é obrigatório")
    Long produtoId,

    // Opcional no create/update: o serviço assume a data de hoje quando vier nula.
    LocalDate dataCompra,

    @Positive(message = "Quantidade de frascos deve ser maior que zero")
    int quantidadeFrascos,

    @NotNull(message = "Preço unitário é obrigatório")
    @PositiveOrZero(message = "Preço unitário não pode ser negativo")
    BigDecimal precoUnitario,

    @PositiveOrZero(message = "Custo adicional não pode ser negativo")
    BigDecimal custoAdicional,

    String fornecedor,
    String observacao,
    boolean ativo,

    // Somente leitura: custo do frasco já com o custo adicional rateado.
    BigDecimal custoEfetivoFrasco,

    // Somente leitura: preenchidos pela auditoria do JPA, ignorados em create/update.
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm,
    Long criadoPor,
    Long atualizadoPor
) {

    public LoteCompraDTO(LoteCompra lote) {
        this(
            lote.getId(),
            lote.getProdutoId(),
            lote.getDataCompra(),
            lote.getQuantidadeFrascos(),
            lote.getPrecoUnitario(),
            lote.getCustoAdicional(),
            lote.getFornecedor(),
            lote.getObservacao(),
            lote.isAtivo(),
            lote.custoEfetivoFrasco(),
            lote.getCriadoEm(),
            lote.getAtualizadoEm(),
            lote.getCriadoPor(),
            lote.getAtualizadoPor()
        );
    }

    public LoteCompra toEntity() {
        LoteCompra lote = new LoteCompra();
        lote.setId(this.id);
        lote.setProdutoId(this.produtoId);
        lote.setDataCompra(this.dataCompra);
        lote.setQuantidadeFrascos(this.quantidadeFrascos);
        lote.setPrecoUnitario(this.precoUnitario);
        lote.setCustoAdicional(this.custoAdicional == null ? BigDecimal.ZERO : this.custoAdicional);
        lote.setFornecedor(this.fornecedor);
        lote.setObservacao(this.observacao);
        lote.setAtivo(this.ativo);
        return lote;
    }
}
