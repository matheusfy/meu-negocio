package meu.negocio.com.br.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import meu.negocio.com.br.entity.Venda;

public record VendaDTO(
    Long id,

    @NotNull(message = "Produto da venda é obrigatório")
    Long produtoId,

    // Opcional no create: o serviço assume a data de hoje quando vier nula.
    LocalDate dataVenda,

    @NotBlank(message = "Tipo da venda é obrigatório (VIDRO_CHEIO ou DECANTE)")
    String tipo,

    // Só para venda de decante (Fase 3); ignorados em VIDRO_CHEIO.
    Long decanteId,
    Long frascoAbertoId,

    @Positive(message = "Quantidade da venda deve ser maior que zero")
    int quantidade,

    @NotNull(message = "Preço unitário é obrigatório")
    @PositiveOrZero(message = "Preço unitário não pode ser negativo")
    BigDecimal precoUnitario,

    String formaPagamento,
    String observacao,

    // Somente leitura: congelados pelo serviço no momento da venda.
    BigDecimal volumeUnitarioMl,
    BigDecimal custoUnitario,
    BigDecimal receitaTotal,
    BigDecimal lucroTotal,
    boolean ativo,

    // Somente leitura: preenchidos pela auditoria do JPA, ignorados em create/update.
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm,
    Long criadoPor,
    Long atualizadoPor
) {

    public VendaDTO(Venda venda) {
        this(
            venda.getId(),
            venda.getProdutoId(),
            venda.getDataVenda(),
            venda.getTipo() == null ? null : venda.getTipo().name(),
            venda.getDecanteId(),
            venda.getFrascoAbertoId(),
            venda.getQuantidade(),
            venda.getPrecoUnitario(),
            venda.getFormaPagamento(),
            venda.getObservacao(),
            venda.getVolumeUnitarioMl(),
            venda.getCustoUnitario(),
            venda.getReceitaTotal(),
            venda.getLucroTotal(),
            venda.isAtivo(),
            venda.getCriadoEm(),
            venda.getAtualizadoEm(),
            venda.getCriadoPor(),
            venda.getAtualizadoPor()
        );
    }

    public Venda toEntity() {
        Venda venda = new Venda();
        venda.setId(this.id);
        venda.setProdutoId(this.produtoId);
        venda.setDataVenda(this.dataVenda);
        venda.setTipo(parseTipo(this.tipo));
        venda.setDecanteId(this.decanteId);
        venda.setFrascoAbertoId(this.frascoAbertoId);
        venda.setQuantidade(this.quantidade);
        venda.setPrecoUnitario(this.precoUnitario);
        venda.setFormaPagamento(this.formaPagamento);
        venda.setObservacao(this.observacao);
        venda.setAtivo(this.ativo);
        return venda;
    }

    private static Venda.TipoVenda parseTipo(String tipo) {
        try {
            return Venda.TipoVenda.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Tipo de venda inválido: use VIDRO_CHEIO ou DECANTE");
        }
    }
}
