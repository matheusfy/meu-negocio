package meu.negocio.com.br.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import meu.negocio.com.br.entity.Decante;

public record DecanteDTO(
    Long id,

    @NotNull(message = "Produto do decante é obrigatório")
    Long produtoId,

    @NotNull(message = "Volume do decante é obrigatório")
    @Positive(message = "Volume do decante deve ser maior que zero")
    BigDecimal volumeMl,

    @PositiveOrZero(message = "Preço de venda não pode ser negativo")
    BigDecimal precoVenda,

    @PositiveOrZero(message = "Custo de embalagem não pode ser negativo")
    BigDecimal custoEmbalagem,

    @PositiveOrZero(message = "Custo de seringa não pode ser negativo")
    BigDecimal custoSeringa,

    @PositiveOrZero(message = "Custo de etiqueta não pode ser negativo")
    BigDecimal custoEtiqueta,

    boolean ativo,

    // Somente leitura: preenchidos pela auditoria do JPA, ignorados em create/update.
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm,
    Long criadoPor,
    Long atualizadoPor
) {

    public DecanteDTO(Decante decante) {
        this(
            decante.getId(),
            decante.getProdutoId(),
            decante.getVolumeMl(),
            decante.getPrecoVenda(),
            decante.getCustoEmbalagem(),
            decante.getCustoSeringa(),
            decante.getCustoEtiqueta(),
            decante.isAtivo(),
            decante.getCriadoEm(),
            decante.getAtualizadoEm(),
            decante.getCriadoPor(),
            decante.getAtualizadoPor()
        );
    }

    public Decante toEntity() {
        Decante decante = new Decante();
        decante.setId(this.id);
        decante.setProdutoId(this.produtoId);
        decante.setVolumeMl(this.volumeMl);
        decante.setPrecoVenda(this.precoVenda);
        decante.setCustoEmbalagem(this.custoEmbalagem);
        decante.setCustoSeringa(this.custoSeringa);
        decante.setCustoEtiqueta(this.custoEtiqueta);
        decante.setAtivo(this.ativo);
        return decante;
    }
}
