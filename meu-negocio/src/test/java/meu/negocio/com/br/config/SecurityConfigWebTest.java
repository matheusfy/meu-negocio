package meu.negocio.com.br.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import meu.negocio.com.br.controller.MarcaController;
import meu.negocio.com.br.controller.SessaoController;
import meu.negocio.com.br.service.MarcaService;

/**
 * Regras de acesso do {@link SecurityConfig}: o que exige login, o que é público,
 * 401 na API, CSRF nas escritas e o login por formulário com usuário configurado.
 */
@WebMvcTest(controllers = {MarcaController.class, SessaoController.class})
@Import(SecurityConfig.class)
class SecurityConfigWebTest {

    // Gerada a cada execução: nenhuma senha (nem falsa) fica versionada.
    private static final String SENHA = UUID.randomUUID().toString();

    @DynamicPropertySource
    static void usuarios(DynamicPropertyRegistry registry) {
        String hash = new BCryptPasswordEncoder().encode(SENHA);
        registry.add("app.seguranca.usuarios[0].id", () -> "7");
        registry.add("app.seguranca.usuarios[0].login", () -> "ana");
        registry.add("app.seguranca.usuarios[0].senha-hash", () -> hash);
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MarcaService marcaService;

    @Test
    void apiSemLoginRespondeUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/marcas")).andExpect(status().isUnauthorized());
    }

    @Test
    void paginaSemLoginRedirecionaParaLogin() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login.html"));
    }

    @Test
    void telaDeLoginEhPublicaEEntregaCookieCsrf() throws Exception {
        mvc.perform(get("/login.html"))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void healthNaoExigeLogin() throws Exception {
        // O actuator não sobe neste slice; o que importa é o Security não barrar (401) nem redirecionar.
        mvc.perform(get("/actuator/health"))
            .andExpect(result -> assertFalse(
                result.getResponse().getStatus() == 401 || result.getResponse().getStatus() / 100 == 3,
                "health exigiu login: " + result.getResponse().getStatus()));
    }

    @Test
    void sessaoMostraUsuarioLogado() throws Exception {
        mvc.perform(get("/api/v1/sessao").with(user("ana")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("ana"));
    }

    @Test
    void escritaSemTokenCsrfEhRecusada() throws Exception {
        mvc.perform(delete("/api/v1/marcas/1").with(user("ana")))
            .andExpect(status().isForbidden());
    }

    @Test
    void escritaComTokenCsrfPassa() throws Exception {
        mvc.perform(delete("/api/v1/marcas/1").with(user("ana")).with(csrf().asHeader()))
            .andExpect(status().isNoContent());
    }

    @Test
    void loginComSenhaCertaAutenticaEVaiParaInicio() throws Exception {
        mvc.perform(formLogin("/login").user("ana").password(SENHA))
            .andExpect(authenticated().withUsername("ana"))
            .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginComSenhaErradaVoltaComErro() throws Exception {
        mvc.perform(formLogin("/login").user("ana").password(SENHA + "-outra"))
            .andExpect(unauthenticated())
            .andExpect(redirectedUrl("/login.html?erro"));
    }
}
