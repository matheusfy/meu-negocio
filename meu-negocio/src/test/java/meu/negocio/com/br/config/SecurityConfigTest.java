package meu.negocio.com.br.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Validação dos usuários configurados em {@link SegurancaProperties}, sem subir contexto Spring.
 */
class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void semUsuariosEComTemporarioPermitidoCriaAdmin() {
        UserDetailsService uds = config.userDetailsService(new SegurancaProperties(List.of(), true), encoder);

        assertEquals("admin", uds.loadUserByUsername("admin").getUsername());
    }

    @Test
    void semUsuariosEmProducaoNaoSobe() {
        SegurancaProperties props = new SegurancaProperties(null, false);

        assertThrows(IllegalStateException.class, () -> config.userDetailsService(props, encoder));
    }

    @Test
    void senhaEmTextoPuroEhRecusada() {
        SegurancaProperties props = new SegurancaProperties(
            List.of(new SegurancaProperties.Usuario(1L, "ana", UUID.randomUUID().toString())), false);

        IllegalStateException erro = assertThrows(IllegalStateException.class,
            () -> config.userDetailsService(props, encoder));
        assertTrue(erro.getMessage().contains("ana"));
    }

    @Test
    void usuarioComHashBcryptEhCarregado() {
        String senha = UUID.randomUUID().toString();
        String hash = encoder.encode(senha);
        SegurancaProperties props = new SegurancaProperties(
            List.of(new SegurancaProperties.Usuario(1L, "ana", hash)), false);

        UserDetailsService uds = config.userDetailsService(props, encoder);

        assertTrue(encoder.matches(senha, uds.loadUserByUsername("ana").getPassword()));
    }
}
