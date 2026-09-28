package meu.negocio.com.br.config;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

/**
 * Login por formulário para os poucos usuários fixos de {@link SegurancaProperties}.
 * A sessão fica no banco (spring-session-jdbc), então sobrevive a restart e deploy.
 */
@Configuration
@EnableConfigurationProperties(SegurancaProperties.class)
public class SecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String PAPEL = "USUARIO";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(SegurancaProperties seguranca, PasswordEncoder encoder) {
        List<UserDetails> usuarios = seguranca.usuarios().stream()
            .map(SecurityConfig::paraUserDetails)
            .toList();
        if (usuarios.isEmpty()) {
            if (!seguranca.permitirUsuarioTemporario()) {
                throw new IllegalStateException(
                    "Nenhum usuário em app.seguranca.usuarios (APP_SEGURANCA_USUARIOS_0_LOGIN etc.)");
            }
            String senha = UUID.randomUUID().toString();
            LOG.warn("Nenhum usuário configurado. Login temporário: admin / {}", senha);
            usuarios = List.of(User.withUsername("admin").password(encoder.encode(senha)).roles(PAPEL).build());
        }
        return new InMemoryUserDetailsManager(usuarios);
    }

    private static UserDetails paraUserDetails(SegurancaProperties.Usuario usuario) {
        if (usuario.login() == null || usuario.login().isBlank()) {
            throw new IllegalStateException("Usuário sem login em app.seguranca.usuarios");
        }
        String hash = usuario.senhaHash();
        if (hash == null || !hash.startsWith("$2")) {
            throw new IllegalStateException(
                "senha-hash do usuário '" + usuario.login() + "' não é um hash BCrypt ($2a$, $2b$ ou $2y$)");
        }
        return User.withUsername(usuario.login()).password(hash).roles(PAPEL).build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // O front é estático e lê o token do cookie XSRF-TOKEN. Nome de atributo null desliga o
        // carregamento preguiçoso, para o cookie já existir ao abrir a tela de login.
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/styles.css", "/error", "/actuator/health", "/actuator/health/**")
                .permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login.html?erro")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html?saiu"))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfHandler))
            // A API responde 401 (o app.js trata); o resto vai para o login, independente do Accept.
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/**"))
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login.html"),
                    AnyRequestMatcher.INSTANCE));
        return http.build();
    }
}
