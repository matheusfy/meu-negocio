package meu.negocio.com.br.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import meu.negocio.com.br.entity.CustoInsumo;

public record CustoInsumoDTO(
    Long id,

    @NotBlank(message = "Nome do insumo é obrigatório")
    String nome,

    @PositiveOrZero(message = "Custo unitário não pode ser negativo")
    BigDecimal custoUnitario,

    String unidade,

    boolean ativo,

    // Somente leitura: preenchidos pela auditoria do JPA, ignorados em create/update.
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm,
    Long criadoPor,
    Long atualizadoPor
) {

    public CustoInsumoDTO(CustoInsumo custoInsumo) {
        this(
            custoInsumo.getId(),
            custoInsumo.getNome(),
            custoInsumo.getCustoUnitario(),
            custoInsumo.getUnidade(),
            custoInsumo.isAtivo(),
            custoInsumo.getCriadoEm(),
            custoInsumo.getAtualizadoEm(),
            custoInsumo.getCriadoPor(),
            custoInsumo.getAtualizadoPor()
        );
    }

    public CustoInsumo toEntity() {
        CustoInsumo custoInsumo = new CustoInsumo();
        custoInsumo.setId(this.id);
        custoInsumo.setNome(this.nome);
        custoInsumo.setCustoUnitario(this.custoUnitario);
        custoInsumo.setUnidade(this.unidade);
        custoInsumo.setAtivo(this.ativo);
        return custoInsumo;
    }
}
