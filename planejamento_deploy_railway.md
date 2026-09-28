# Planejamento — Subir o app no Railway com banco no Supabase

> Objetivo: tirar o Ateliê de Decantes do PC de casa e deixá-lo acessível pela internet
> (celular fora de casa, a mãe em outro lugar), no mesmo modelo do jpGakushuu:
> **app no Railway** + **Postgres no Supabase**.

## Como está agora

- Spring Boot (Java 21, Gradle) serve **front estático + API REST** na porta 8080.
- Banco: Postgres local via `spring-boot-docker-compose` (só em dev). Migrations Flyway V1–V8.
- Sessão HTTP já é persistida em banco (`spring-session-jdbc`, tabelas no V1) — bom para nuvem.
- `application-prod.properties` (ainda não commitado) já lê o datasource de
  `SPRING_DATASOURCE_*` e liga `Cache-Control: no-cache`. Não tem segredo, pode ir pro git.
- **Não há login.** Qualquer um que souber a URL lê e grava tudo. Isso é o bloqueador nº 1.

## Arquitetura alvo

```
Celular / PC ──HTTPS──▶ Railway (container Docker: java -jar meu-negocio.jar, profile prod)
                              │  JDBC + SSL (pooler do Supabase, session mode, porta 5432)
                              ▼
                        Supabase Postgres (projeto próprio "meu-negocio")
```

- Deploy automático do Railway a partir da branch **`main`** (fluxo atual: feature → `develop` → `main`),
  com "Wait for CI" ligado para só publicar se o CI passar.
- HTTPS e domínio `*.up.railway.app` vêm de graça do Railway.

## Decisões (recomendação — confirmar antes de começar)

| # | Decisão | Recomendação | Por quê |
|---|---------|--------------|---------|
| 1 | Login antes de publicar? | **Sim, obrigatório** (Fase 2 antes da Fase 4) | Sem login a URL pública expõe e permite apagar todos os dados. |
| 2 | Tipo de login | Spring Security, **form login**, 2 usuários fixos (você + mãe) com senha em hash BCrypt vinda de variável de ambiente | Simples; multiusuário de verdade só se virar produto (já decidido no plano de rede local). |
| 3 | Projeto Supabase | **Projeto novo e separado** do jpGakushuu | Isola dados e senha. Atenção: o plano free permite **2 projetos ativos** — se já houver 2, reaproveitar com schema próprio. |
| 4 | Conexão ao Supabase | **Supavisor pooler em session mode** (`aws-0-<região>.pooler.supabase.com:5432`, usuário `postgres.<ref>`) | A conexão direta `db.<ref>.supabase.co` é só IPv6 e o Railway sai por IPv4. O transaction mode (6543) quebra prepared statements do Hibernate/Flyway. |
| 5 | Região | ✅ **Supabase `sa-east-1` (São Paulo)** e Railway na **mesma região do serviço do jpGakushuu** (conferir no painel do Railway) | Confirmado em 2026-09-28. Mesmo pooler do jpGakushuu: `aws-1-sa-east-1.pooler.supabase.com`. |
| 6 | Dados atuais | ✅ **Migrar** os dados do Postgres local com `pg_dump --data-only` | Confirmado em 2026-09-28: há dados reais. Flyway cria o schema no Supabase; só os dados são copiados. |
| 7 | Versão do Spring Boot | Trocar `4.1.1-SNAPSHOT` pela **última GA 4.1.x** e remover o repo de snapshots | Build de produção não pode depender de snapshot que muda a cada dia. |

## Fase 1 — App pronto para nuvem

Branch: `issue-7-DeployPreparo`. **Implementada e testada** (2026-09-28): imagem de 566 MB, sobe em ~4 s usando ~360 MB de RAM,
Flyway aplica as migrations num Postgres vazio, `/actuator/health` = UP, roda como usuário não-root
e falha na inicialização se faltar `SPRING_DATASOURCE_URL`.

1. **Fixar versão estável do Spring Boot** (decisão 7). Rodar os testes.
2. **Porta dinâmica**: `server.port=${PORT:8080}` (Railway injeta `PORT`).
3. **Commitar `application-prod.properties`** e completar:
   ```properties
   spring.datasource.url=${SPRING_DATASOURCE_URL}          # sem default em prod: falha cedo se faltar
   spring.datasource.hikari.maximum-pool-size=5             # limite de conexões do Supabase free
   server.forward-headers-strategy=framework                # Railway termina o HTTPS no proxy
   server.servlet.session.cookie.secure=true
   server.servlet.session.cookie.same-site=lax
   spring.h2.console.enabled=false
   management.endpoints.web.exposure.include=health
   ```
4. **Health check**: adicionar `spring-boot-starter-actuator`; Railway usa `/actuator/health`.
5. **Dockerfile** multi-stage em `meu-negocio/`:
   - estágio build: `eclipse-temurin:21-jdk`, `./gradlew bootJar --no-daemon`
     — **usar `bootJar`, não `build`**: `build` depende de `installGitHooks`, que roda
     `git config` e falha dentro do Docker (não há `.git`).
   - estágio runtime: `eclipse-temurin:21-jre`, usuário não-root,
     `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75`, `ENTRYPOINT java -jar app.jar`.
   - `.dockerignore` com `build/`, `.gradle/`, `bin/`.
6. **Fuso horário**: `TZ=America/Sao_Paulo` no container (datas de venda/lote).
7. Testar local: `docker build` + `docker run` apontando para o Postgres do compose.

## Fase 2 — Login (bloqueador de publicação)

Branch: `issue-7-Login`. **Implementada e testada** (2026-09-28).

- `SecurityConfig` + `SegurancaProperties`: form login (`/login.html`), usuários fixos lidos de
  `app.seguranca.usuarios[n]` com senha em **hash BCrypt**. Hash inválido (ex.: senha em texto)
  impede o app de subir.
- Sessão de **30 dias** guardada no banco (spring-session-jdbc): sobrevive a restart/deploy,
  então não precisou de remember-me.
- Públicos: `/login.html`, `/styles.css`, `/actuator/health`. API sem login → **401**
  (o `app.js` volta para o login); páginas → redirect para `/login.html`.
- **CSRF** ligado: token no cookie `XSRF-TOKEN`, enviado no header `X-XSRF-TOKEN` pelo `app.js`
  e no campo `_csrf` pelo formulário de login/logout.
- Barra lateral mostra quem está logado (`GET /api/v1/sessao`) e tem botão **Sair**.
- `criado_por`/`atualizado_por` passam a gravar o `id` do usuário logado.
- Dev: sem usuários configurados, o log mostra um login temporário `admin` com senha aleatória.
  No perfil `prod` isso é desligado — sem usuários o app não sobe.
- Testado em container com perfil prod: redirect, 401, senha errada, login sem CSRF (403),
  POST com/sem CSRF, sessão após `docker restart`, logout.

**Gerar o hash de uma senha** (a senha em texto não sai do seu PC; com Docker Desktop aberto):
```
docker run --rm httpd:alpine htpasswd -nbBC 10 "" 'SUA_SENHA' | tr -d ':
'
```
O resultado começa com `$2y$10$...` e é o valor de `APP_SEGURANCA_USUARIOS_n_SENHAHASH`.

## Fase 3 — Supabase

1. Criar projeto `meu-negocio` (decisões 3 e 5), senha forte do banco guardada no gerenciador de senhas.
2. **Desligar a Data API** (Settings → API) **ou** criar migration `V9__enable_rls.sql`
   ativando RLS em todas as tabelas: o Supabase expõe o schema `public` via REST com a chave
   anon, e as tabelas criadas pelo Flyway ficam sem RLS. O app conecta como `postgres`
   (dono das tabelas), então RLS sem policies não o afeta.
3. Pegar a string de conexão **Session pooler** → montar
   `jdbc:postgresql://aws-1-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require`
   (o prefixo `aws-0`/`aws-1` pode variar: copiar o host exato do painel).
4. Primeira subida: o Flyway roda V1–V8 sozinho quando o app conectar.

## Fase 4 — Railway

1. New Project → Deploy from GitHub → `matheusfy/meu-negocio`, **Root Directory `meu-negocio`**,
   builder Dockerfile, branch `main`, ligar **Wait for CI**.
2. Variáveis (Railway → Variables; nunca no git — lembrar do GitGuardian):
   ```
   SPRING_PROFILES_ACTIVE=prod
   SPRING_DATASOURCE_URL=jdbc:postgresql://...pooler.supabase.com:5432/postgres?sslmode=require
   SPRING_DATASOURCE_USERNAME=postgres.<ref>
   SPRING_DATASOURCE_PASSWORD=<senha>
   APP_SEGURANCA_USUARIOS_0_ID=1
   APP_SEGURANCA_USUARIOS_0_LOGIN=<seu login>
   APP_SEGURANCA_USUARIOS_0_SENHAHASH=$2y$10$...
   APP_SEGURANCA_USUARIOS_1_ID=2
   APP_SEGURANCA_USUARIOS_1_LOGIN=<login da mãe>
   APP_SEGURANCA_USUARIOS_1_SENHAHASH=$2y$10$...
   TZ=America/Sao_Paulo
   ```
3. Healthcheck e restart já estão em `meu-negocio/railway.json`. O Railway **não** procura esse
   arquivo dentro do Root Directory: em Settings → Config-as-code, apontar para `/meu-negocio/railway.json`.
4. Gerar domínio público (Settings → Networking → Generate Domain).
5. Plano: Hobby (US$ 5/mês de crédito) cobre uma JVM pequena; acompanhar uso de memória
   na primeira semana.

## Fase 5 — Dados e backup

1. **Migrar dados** do Postgres local, depois do Flyway rodar no Supabase.
   Pré-requisito: `main` atualizada com tudo de `develop` (PR #54 *Develop to Main* e Vendas #64),
   senão o schema do Supabase fica atrás do banco local:
   ```
   docker compose exec postgres pg_dump -U myuser --data-only \
     --exclude-table=flyway_schema_history --exclude-table='spring_session*' mydatabase > dados.sql
   psql "<URL do Supabase>" -f dados.sql
   ```
   Depois ajustar as sequences (`setval`) de cada tabela com `id` serial/identity.
   `dados.sql` fora do git (apagar depois).
2. **Backup**: o Supabase free não oferece backup baixável. Criar workflow
   `.github/workflows/backup.yml` semanal (`pg_dump` → artefato do Actions com retenção 30 dias),
   URL do banco como secret do GitHub.
3. **Pausa por inatividade**: o Supabase free pausa após 7 dias sem uso. Com uso normal não
   acontece; se acontecer, é só reativar no painel (o app volta sozinho).

## Fase 6 — Go-live (checklist)

- [ ] Abrir a URL em aba anônima → cai no login; API sem sessão responde 401.
- [ ] Login com os 2 usuários no PC e no celular; "Adicionar à tela inicial".
- [ ] Cadastrar produto, lote, venda; conferir estoque e resultado.
- [ ] Reiniciar o serviço no Railway → sessão continua (spring-session-jdbc) e dados persistem.
- [ ] Merge em `main` com CI vermelho **não** gera deploy.
- [ ] Rodar o backup manualmente uma vez e conferir o artefato.
- [ ] `curl` direto na REST do Supabase com a chave anon **não** retorna dados.
- [ ] Atualizar README e `planejamento_acesso_rede_local.md` apontando para este plano.

## Fora do escopo (por agora)

- Domínio próprio (dá para apontar depois no Railway).
- Ambiente de homologação (`develop` não é publicada — igual ao jpGakushuu).
- Multiusuário / cadastro de usuários.
- Observabilidade além dos logs do Railway.
