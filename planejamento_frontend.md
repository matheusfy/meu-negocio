# Planejamento — Front-end do MeuNegócio ("Ateliê de Decantes")

> Objetivo: uma interface simples, em português, para a **usuária final (não-técnica)**
> cadastrar perfumes, insumos e decantes, calcular custos e enxergar a viabilidade
> de transformar cada perfume em decantes de 3 / 5 / 10 ml.
>
> Escopo desta primeira versão (decidido): **só o que a API já faz hoje** — cadastros
> + `GET /decantes/{id}/viabilidade` + `GET /produtos/{id}/viabilidade-decantes`.
> Nada de impostos, frete, estoque, metas mensais ou login (ver §8).

**Prévia navegável:** https://claude.ai/code/artifact/4fe4459c-9e41-4f95-b81f-72b291045e5a
Mockup clicável, dados fictícios, mas o cálculo é real e espelha o `AnaliseViabilidadeService`.

---

## 1. Duas entregas — as duas planejadas aqui

A pedido: planejar **tanto** a versão estática quanto uma **SPA React separada** (`frontend/`),
já pensando que isso pode virar produto.

### 1.1 Opção A — Estático dentro do Spring Boot

| Item | Detalhe |
|---|---|
| Onde | `meu-negocio/src/main/resources/static/` (`index.html`, `app.js`, `styles.css`) |
| Stack | HTML + CSS + JavaScript puro (opcionalmente Alpine.js via CDN). **Zero build.** |
| Como roda | A própria aplicação serve em `http://localhost:8080/`. Mesma origem → **sem CORS**. |
| Deploy | Já vai dentro do `.jar` gerado pelo `./gradlew build`. |
| CI | Nada muda no `ci.yml`. |
| Prós | Entrega em **1 PR**; nada de Node; fácil de manter enquanto se está aprendendo Java; a mãe usa hoje. |
| Contras | Sem componentização real; JS "na mão" cresce mal; ruim de escalar se virar produto. |

### 1.2 Opção B — SPA React separada (`frontend/`)

| Item | Detalhe |
|---|---|
| Onde | `frontend/` na raiz do repositório (monorepo leve: back em `meu-negocio/`, front em `frontend/`). |
| Stack | **Vite + React + TypeScript**, React Router, TanStack Query (cache/estado de servidor), `react-hook-form` + `zod` (formulários/validação). Estilo: CSS Modules ou Tailwind. |
| Dev | `npm run dev` na porta 5173; proxy `/api` → `http://localhost:8080` no `vite.config.ts` (sem CORS em dev). |
| Prod | Duas rotas possíveis: **(b1)** `npm run build` e copiar `dist/` para `meu-negocio/src/main/resources/static/` (continua 1 jar, sem CORS, sem host separado); **(b2)** deploy independente (Vercel/Netlify) apontando para a API — aí **precisa CORS** no back. |
| CI | Novo job `frontend` no `ci.yml`: `node`, `npm ci`, `npm run lint`, `npm run build`, `npm test` (Vitest). |
| Prós | Base de produto de verdade: componentes reutilizáveis, testes de UI, tipos compartilhados com o contrato da API, deploy independente possível. |
| Contras | Mais peças móveis; setup inicial maior; **mexe no CI, que já é sensível** (ver memória do fluxo de PR). |

### 1.3 Caminho recomendado

**Ir direto pela Opção B (`frontend/` React), entregando em produção pela rota b1**
(build copiado para `resources/static`, servido pelo mesmo jar).

Racional:
- O trabalho de **UI/telas/design é o mesmo** nos dois caminhos — fazendo em React, nada é jogado fora quando virar produto.
- A rota b1 dá o melhor dos dois mundos no começo: **1 artefato de deploy, sem CORS, sem host novo**, e ainda assim código componentizado.
- Separar o deploy (b2) e ligar CORS fica para quando realmente existir um "produto" com usuários fora de casa.
- Se o setup do React travar o ritmo, a **Opção A é o fallback** e destrava a mãe usando em dias — as telas e o design deste plano servem igual.

O resto do documento (telas, contrato, design) vale para **as duas opções**.

---

## 2. Mapa de telas

| Tela | Rota (React) | O que faz | Endpoints consumidos |
|---|---|---|---|
| **Início** | `/` | Resumo (nº de perfumes, nº de decantes, total investido) + "melhor oportunidade agora" + lista de perfumes | `GET /produtos`, `GET /produtos/{id}/viabilidade-decantes` |
| **Perfumes** | `/perfumes` | Lista em cards + formulário "adicionar perfume" | `GET/POST/PUT/DELETE /produtos` |
| **Perfume (detalhe)** | `/perfumes/:id` | Dados do perfume, custo/ml, decantes já configurados, botão "comparar tamanhos", form "novo decante" | `GET /produtos/{id}`, `GET /decantes?produtoId={id}`, `POST /decantes` |
| **Insumos** | `/insumos` | Tabela do catálogo (frasco, seringa, etiqueta...) + form. É o "levantamento de custos". | `GET/POST/PUT/DELETE /insumos` |
| **Decantes** | `/decantes` | Visão geral de todos os decantes agrupados por perfume (preço, custo, margem) | `GET /decantes`, `GET /produtos` |
| **Viabilidade** | `/perfumes/:id/viabilidade` | **Tela principal.** Ficha do perfime + comparação lado a lado 3/5/10 ml + selos (melhor margem / maior lucro por unidade / melhor retorno) + cálculo passo a passo + veredito em texto | `GET /produtos/{id}/viabilidade-decantes`, `GET /decantes/{id}/viabilidade` |

Fluxo típico da mãe: **Início → clica no perfume → vê os decantes → "Comparar tamanhos" → decide.**

---

## 3. Contrato com a API (o que o front consome hoje)

Campos vindos do código atual (`dto/`):

- **`ProdutoDTO`**: `id, nome, marca, categoria, sku, fornecedorPrincipalId, precoVenda, precoCusto, volumeMl, estoqueAtual, estoqueMinimo, ativo` + auditoria (`criadoEm, atualizadoEm, criadoPor, atualizadoPor` — **somente leitura, não enviar**).
  - Para a viabilidade não estourar 400, `precoCusto` e `volumeMl` **precisam estar preenchidos e > 0**.
- **`CustoInsumoDTO`**: `id, nome, custoUnitario, unidade, ativo` + auditoria.
- **`DecanteDTO`**: `id, produtoId (obrigatório), volumeMl (obrigatório, > 0), precoVenda, custoEmbalagem, custoSeringa, custoEtiqueta, ativo` + auditoria.
- **`AnaliseViabilidade`** (resposta de `/decantes/{id}/viabilidade`): `produtoId, decanteId, custoPorMl, custoProdutoNoDecante, custoEmbalagem, custoTotal, precoVenda, lucroUnitario, margem, quantidadeDecantes, investimentoLote, receitaTotal, lucroTotal, margemLote`.
- **`ComparacaoDecantes`** (resposta de `/produtos/{id}/viabilidade-decantes`): `produtoId, analises[], decanteMelhorMargem, decanteMaiorLucroUnitario, decanteMelhorRetornoInvestimento` (os três últimos podem vir `null` se não houver decante ativo).

Outros pontos:
- **Paginação**: as listagens retornam `Page<>` do Spring → objeto com `content[]`, `totalElements`, `totalPages`, `number`, `size`. O front manda `?page=&size=&sort=`. Para o uso da mãe, `size` alto (ex.: 100) já resolve; paginação de verdade fica para depois.
- **Erros**: `ErroResposta` = `{ timestamp, status, erro, mensagem, caminho, detalhes[] }`. O front mostra `mensagem` no topo do formulário e `detalhes[]` (ex.: `"volumeMl: deve ser maior que zero"`) campo a campo.
- **Sem autenticação** hoje (não há Spring Security). `user_id` é fixo em `1` no banco. OK para uso local; ver §8.

---

## 4. Ajustes necessários no back-end

Poucos, e só alguns dependem da opção escolhida:

1. **Servir o front (opções A e B-b1)** — garantir que o Spring entregue `index.html` na raiz e faça *fallback* das rotas do React Router para `index.html` (senão dar F5 em `/perfumes` dá 404). Um `WebMvcConfigurer` com `resourceHandler` + `PathResourceResolver` que devolve `index.html` quando o recurso não existe, ou um controller de fallback simples.
2. **CORS (só opção B-b2, deploy separado)** — `WebMvcConfigurer#addCorsMappings` liberando a origem do front (`http://localhost:5173` em dev e o domínio de produção). Não precisa se o front for servido pelo mesmo jar.
3. **(Opcional) endpoint de resumo para o Início** — hoje o dashboard precisaria de várias chamadas (`/produtos` + um `/viabilidade-decantes` por perfume). Um `GET /api/v1/resumo` devolvendo os números da home evita o N+1. Pode ficar para a fase de polimento.
4. **Nada de auth / multiusuário agora** — fora de escopo (§8).
5. **CORS/headers de erro já OK** — o `GlobalExceptionHandler` já devolve JSON estruturado para 400/404/409/500; o front só precisa consumir.

---

## 5. Design / identidade visual

Direção escolhida: **"perfumaria de bancada / apothecary"** — âmbar queimado, terracota e
dourado antigo sobre papel quente; um verde-erva como cor **semântica** de "vale a pena".
Aconchegante, com personalidade de marca, mas simples de operar.

**Paleta (tokens CSS):**

| Papel | Claro | Escuro |
|---|---|---|
| Fundo | `#F7F1E6` | `#1B1510` |
| Superfície / card | `#FCF7EC` | `#241C15` |
| Texto | `#2B2018` | `#F1E5D3` |
| Acento (âmbar queimado) | `#AF5828` | `#D67F48` |
| Acento profundo | `#7C3A18` | `#EBA675` |
| Dourado (só "melhor cenário") | `#B58A38` | `#D8B368` |
| Verde-erva (semântico "viável") | `#5D6E45` | `#9EAF7C` |
| Sangue-de-boi (semântico "não compensa") | `#8C3A2C` | `#DB8A77` |

**Tipografia:**
- **Fraunces** (serifada com personalidade) — títulos de tela e números grandes.
- **Karla** (sans humanista) — interface e texto corrido.
- **Spline Sans Mono** — bloco de "cálculo passo a passo" (cara de receita/fórmula).

**Componentes-chave:**
- *Tile* de resumo (número grande em Fraunces, rótulo em maiúsculas).
- *Card de perfume* com chips (custo, volume, custo/ml).
- *Vial card* (cartão-frasco) para a comparação — um por tamanho, com selo dourado no cenário vencedor.
- *Bloco de cálculo* (mono) mostrando a conta linha a linha, como no `planejamento_sistema_viabilidade_decantes.md` §6–13.
- *Veredito* em uma frase, em linguagem de dona de negócio ("...rende cerca de R$ 562 sobre R$ 593 investidos").
- Formulário de **uma coluna**, campos grandes, ajuda inline, confirmação antes de excluir.

**Acessível e mobile-first:** a mãe provavelmente usa o **celular** → layout de 1 coluna no telefone,
menu vira barra horizontal; contraste AA; foco visível; `prefers-reduced-motion` respeitado;
zero jargão técnico na tela ("preço de custo do frasco", não "precoCusto").

---

## 6. Fases de entrega (no fluxo de PR de vocês)

Cada fase = 1 branch `issue-N-...` → build local → PR para `develop` → pipeline verde → squash-merge
(ver `CONTRIBUTING.md` e a memória do fluxo).

| Fase | Entrega | PR |
|---|---|---|
| **F0** *(só na opção B)* | Scaffold `frontend/` (Vite + React + TS), `vite.config.ts` com proxy `/api`, ESLint/Prettier, job `frontend` no `ci.yml`, fallback SPA no Spring | 1 PR |
| **F1** | Esqueleto de navegação + tema/tokens + tela **Início** + **Perfumes** (listar / criar / editar / excluir) | 1 PR |
| **F2** | **Insumos** (CRUD) + **Decante** (config por perfume, form "novo decante") + tela **Decantes** (visão geral) | 1 PR |
| **F3** | **Viabilidade**: ficha do perfume + comparação 3/5/10 ml + selos + cálculo passo a passo + veredito | 1 PR (o coração) |
| **F4** | Polimento: responsivo fino, estados vazios, erros amigáveis, *loading*, endpoint `/resumo` opcional | 1 PR |

Dá para começar F1 sem F0 se for a Opção A.

---

## 7. Riscos / atenção

- **CI sensível** (memória): F0 mexe no `ci.yml` — validar o job novo isolado antes de somar telas.
- **GitGuardian**: não colocar nenhuma chave/URL com credencial em arquivos de config do front.
- **`.claude/settings.local.json`** continua sempre `M` — não commitar junto.
- **Spring Boot 4 é SNAPSHOT** — o fallback de rota SPA usa API estável de `WebMvcConfigurer`, deve seguir válido.
- **Números com `BigDecimal` no back / `number` no front** — formatar sempre com `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`; nunca fazer conta financeira nova no front (o back já calcula).

---

## 8. Fora de escopo agora (alinhado com a Fase E do back-end)

Perda de produção · impostos / taxas de marketplace / frete / comissão · estoque de insumos com baixa automática ·
histórico venda real × estimado · metas de faturamento mensal / ROI / ponto de equilíbrio / ticket médio ·
login / multiusuário / perfis.

Quando a Fase E do back-end existir, entra uma **F5** no front (tela de "Planejamento do mês").
