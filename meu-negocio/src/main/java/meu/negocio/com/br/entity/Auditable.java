package meu.negocio.com.br.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

/**
 * Superclasse mapeada com os campos padrão de auditoria (quem e quando criou/alterou o registro).
 * Os valores são preenchidos automaticamente pelo Spring Data JPA via {@link AuditingEntityListener},
 * por isso não expõe setters públicos.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @CreatedBy
    @Column(name = "criado_por", updatable = false)
    private Long criadoPor;

    @LastModifiedBy
    @Column(name = "atualizado_por")
    private Long atualizadoPor;

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public Long getCriadoPor() {
        return criadoPor;
    }

    public Long getAtualizadoPor() {
        return atualizadoPor;
    }
}
