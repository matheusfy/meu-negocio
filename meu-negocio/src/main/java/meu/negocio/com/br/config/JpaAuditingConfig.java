package meu.negocio.com.br.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    // Ainda não há autenticação implementada; usamos um usuário de sistema fixo.
    // TODO: substituir por SecurityContextHolder assim que o login existir.
    private static final Long USUARIO_SISTEMA_PADRAO = 1L;

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> Optional.of(USUARIO_SISTEMA_PADRAO);
    }
}
