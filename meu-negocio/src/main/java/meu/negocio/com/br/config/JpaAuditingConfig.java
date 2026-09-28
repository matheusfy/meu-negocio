package meu.negocio.com.br.config;

import java.util.Objects;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    // Usado quando não há usuário logado ou o usuário logado não tem id configurado.
    private static final Long USUARIO_SISTEMA_PADRAO = 1L;

    @Bean
    public AuditorAware<Long> auditorAware(SegurancaProperties seguranca) {
        return () -> Optional.of(idDoUsuarioLogado(seguranca));
    }

    private static Long idDoUsuarioLogado(SegurancaProperties seguranca) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return USUARIO_SISTEMA_PADRAO;
        }
        return seguranca.usuarios().stream()
            .filter(u -> auth.getName().equals(u.login()))
            .map(SegurancaProperties.Usuario::id)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(USUARIO_SISTEMA_PADRAO);
    }
}
