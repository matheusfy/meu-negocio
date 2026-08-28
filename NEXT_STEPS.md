# Próximos passos

## Git / CI
- [ ] Configurar branch protection no GitHub para `develop` (PR obrigatório + checks `Build & Testes` e `Checkstyle`) — ver `CONTRIBUTING.md`
- [ ] Configurar branch protection no GitHub para `main` (mesma regra)
- [ ] Abrir uma PR de teste (`feature/algo` → `develop`) para o workflow `ci.yml` rodar pelo menos uma vez — os checks só aparecem como opção de "required status check" depois disso
- [ ] Depois que os checks aparecerem, marcá-los como obrigatórios nas duas regras acima

## Ambiente local
- [ ] Ter o Docker Desktop aberto antes de rodar a aplicação (o `spring-boot-docker-compose` sobe o Postgres do `compose.yaml` automaticamente)
- [ ] Rodar `./gradlew build` uma vez localmente para validar que o build + testes + checkstyle passam antes da primeira PR

## Projeto (curto prazo)
- [ ] Criar `src/main/resources/db/migration` e a primeira migration Flyway quando a primeira entidade for modelada
- [ ] Preencher `application.properties` com config básica de JPA/Flyway (ex: `spring.jpa.hibernate.ddl-auto=validate`)
- [ ] Corrigir o README: hoje cita Maven, mas o projeto usa Gradle (`./gradlew`)

## Roadmap (ver `planejamento_sistema_viabilidade_decantes.md`)
Fase 1 do `planejamento_sistema_gestao_rentabilidade.md` foi refinada para o simulador de viabilidade de decantes. `Produto` (com `volumeMl`) já está implementado; falta:
- [x] Fase A: `CustoInsumo` (catálogo de insumos — frasco, seringa, etiqueta) — CRUD REST em `/api/v1/insumos`
- [ ] Fase B: `Decante` (configuração de venda por tamanho, ligado a um `Produto`)
- [ ] Fase C: `AnaliseViabilidadeService` (cálculo de custo por ml, lucro, margem, comparação entre tamanhos)
- [ ] Fase D: testes unitários (`AnaliseViabilidadeService` primeiro, é o coração do sistema)
- [ ] Fase E (backlog): perda de produção, impostos/taxas/frete/comissão, estoque de insumos, histórico de venda real vs. estimado
