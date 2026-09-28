package meu.negocio.com.br.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

import meu.negocio.com.br.entity.Marca;

public record MarcaDTO(
    Long id,

    @NotBlank(message = "Nome da marca é obrigatório")
    String nome,

    String pais,

    boolean ativo,

    // Somente leitura: preenchidos pela auditoria do JPA, ignorados em create/update.
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm,
    Long criadoPor,
    Long atualizadoPor
) {

    public MarcaDTO(Marca marca) {
        this(
            marca.getId(),
            marca.getNome(),
            marca.getPais(),
            marca.isAtivo(),
            marca.getCriadoEm(),
            marca.getAtualizadoEm(),
            marca.getCriadoPor(),
            marca.getAtualizadoPor()
        );
    }

    public Marca toEntity() {
        Marca marca = new Marca();
        marca.setId(this.id);
        marca.setNome(this.nome);
        marca.setPais(this.pais);
        marca.setAtivo(this.ativo);
        return marca;
    }
}
