package meu.negocio.com.br.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import meu.negocio.com.br.entity.FrascoAberto;

public record FrascoAbertoDTO(
    Long id,

    @NotNull(message = "Produto do frasco é obrigatório")
    Long produtoId,

    // Opcional no create: o serviço assume a data de hoje quando vier nula.
    LocalDate dataAbertura,

    String observacao,

    // Somente leitura: preenchidos pelo serviço a partir do produto e do consumo.
    BigDecimal volumeInicialMl,
    BigDecimal mlRestante,
    String status,

    // Somente leitura: preenchidos pela auditoria do JPA, ignorados em create.
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm,
    Long criadoPor,
    Long atualizadoPor
) {

    public FrascoAbertoDTO(FrascoAberto frasco) {
        this(
            frasco.getId(),
            frasco.getProdutoId(),
            frasco.getDataAbertura(),
            frasco.getObservacao(),
            frasco.getVolumeInicialMl(),
            frasco.getMlRestante(),
            frasco.getStatus() == null ? null : frasco.getStatus().name(),
            frasco.getCriadoEm(),
            frasco.getAtualizadoEm(),
            frasco.getCriadoPor(),
            frasco.getAtualizadoPor()
        );
    }
}
