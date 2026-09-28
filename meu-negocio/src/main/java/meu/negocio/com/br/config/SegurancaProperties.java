package meu.negocio.com.br.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Usuários que podem entrar no app, lidos de {@code app.seguranca.*}.
 *
 * <p>Em produção vêm de variáveis de ambiente (nunca do git), por exemplo:
 * {@code APP_SEGURANCA_USUARIOS_0_ID=1}, {@code APP_SEGURANCA_USUARIOS_0_LOGIN=matheus},
 * {@code APP_SEGURANCA_USUARIOS_0_SENHAHASH=$2a$10$...} (hash BCrypt, nunca a senha).
 *
 * @param usuarios                  usuários fixos; vazio só é aceito com usuário temporário permitido
 * @param permitirUsuarioTemporario se não houver usuários, cria um "admin" com senha aleatória no log
 *                                  (conveniência de desenvolvimento; desligado no perfil prod)
 */
@ConfigurationProperties(prefix = "app.seguranca")
public record SegurancaProperties(
    List<Usuario> usuarios,
    @DefaultValue("true") boolean permitirUsuarioTemporario
) {

    public SegurancaProperties {
        usuarios = usuarios == null ? List.of() : List.copyOf(usuarios);
    }

    /**
     * @param id        id gravado em criado_por/atualizado_por
     * @param login     nome usado na tela de login
     * @param senhaHash hash BCrypt da senha
     */
    public record Usuario(Long id, String login, String senhaHash) {
    }
}
