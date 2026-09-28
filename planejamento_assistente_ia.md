# Planejamento — Assistente / Analisador de IA

> Objetivo: além dos números, o app dar **sugestões** e responder perguntas em
> linguagem simples sobre o negócio de decantes da mãe.

## Duas camadas

| | Camada 1 — Analisador (regras) | Camada 2 — Assistente (LLM) |
|---|---|---|
| O que é | Conselhos calculados a partir da viabilidade | Chat: a mãe pergunta em texto livre |
| Estado | **Implementada** neste PR (`RecomendacaoService`, `GET /api/v1/produtos/{id}/analise`) | **Este plano** — PR futuro |
| Custo | Zero | Centavos por pergunta |
| Depende de | Nada | Chave de API da Anthropic + dependência nova |

A Camada 1 aparece fixa na tela de Viabilidade. A Camada 2 é um extra que usa a
Camada 1 e os dados da mãe como contexto.

---

## Camada 1 — Analisador (feito)

`RecomendacaoService.recomendar(ComparacaoDecantes)` devolve `List<Recomendacao>`
(`nivel` BOM/ATENCAO/RUIM/INFO, `titulo`, `texto`, `acao`). Regras atuais:

- Melhor tamanho (maior margem de lote) com o resumo em reais.
- Margem baixa em todos os tamanhos → RUIM; melhor margem < 40% → ATENÇÃO.
- Ponto de equilíbrio: paga o frasco vendendo ≤ 50% do lote → BOM; ≥ 65% → ATENÇÃO.
- Troca "pequeno rende mais / grande dá menos trabalho".
- Decante sem preço de venda → aviso acionável.
- Lembrete de que é lucro bruto (faltam taxas de marketplace, frete, envio).

Faixas (40% / 25% / 50% / 65%) são constantes em `RecomendacaoService` — dá para
externalizar para `application.properties` depois.

---

## Camada 2 — Assistente (LLM)

### Arquitetura

```
Front  →  POST /api/v1/assistente/perguntar   (SSE para streaming da resposta)
Back   →  AssistenteService
           - monta system prompt (fixo) + bloco DADOS (cadastro atual, JSON compacto)
           - chama a Messages API da Anthropic com ferramentas (tool use)
           - executa a ferramenta chamada (envolve os Services existentes)
           - devolve o texto ao front
Chave  →  ANTHROPIC_API_KEY como variável de ambiente do servidor. Nunca no front.
```

- **SDK:** `implementation("com.anthropic:anthropic-java:2.34.0")`, `AnthropicOkHttpClient.fromEnv()`.
- **Modelo:** o guia da Anthropic manda `claude-opus-5` por padrão. Para este caso
  (perguntas simples, uma usuária, baixo risco) provavelmente `claude-haiku-4-5`
  (~US$1 in / US$5 out por milhão de tokens) ou `claude-sonnet-5`. **Decisão do dono.**
  Estimativa: system ~1,5k + dados ~1k + pergunta ~50 + resposta ~400 tokens →
  Haiku ≈ US$0,004/pergunta; Opus 5 ≈ US$0,02. Para o volume de uma pessoa, centavos/mês.
- **Streaming** (`.stream()` + `.get_final_message()` equivalente em Java) e
  `thinking: adaptive` ligados.
- **Prompt caching**: o system prompt é fixo → `CacheControlEphemeral` TTL 1h nele;
  o bloco DADOS (muda a cada pergunta) vai **depois** do breakpoint, como primeira
  mensagem do usuário.

### Tool use, não MCP (por ora)

O cliente da mãe é o próprio app; os Services rodam na mesma JVM. MCP só valeria se a
interface fosse o Claude Desktop ou se as ferramentas fossem reusadas por vários hosts.
Se a camada de serviço ficar limpa, expor como servidor MCP depois é um passo pequeno
(Spring AI tem suporte). **Anotado como opção futura.**

### Ferramentas

| Ferramenta | Faz | Envolve |
|---|---|---|
| `listar_perfumes()` | perfumes cadastrados | `ProdutoService` |
| `detalhar_perfume(id)` | dados + custo/ml + decantes | `ProdutoService` + `DecanteService` |
| `listar_insumos()` | catálogo de custos | `CustoInsumoService` |
| `simular_viabilidade(precoCusto, volumeMl, volumeDecante, precoVenda, custoFrasco, custoSeringa, custoEtiqueta)` | cálculo **sem salvar** | `AnaliseViabilidadeService.analisar(...)` (já é puro) |
| `comparar_tamanhos(produtoId)` | os tamanhos + vencedores + recomendações | `AnaliseViabilidadeService` + `RecomendacaoService` |
| `criar_perfume(...)` / `criar_decante(...)` | **grava** — atrás de confirmação | `ProdutoService` / `DecanteService` |

`criar_*` só executam depois de a assistente perguntar *"vou cadastrar X, confirma?"* e
a mãe dizer sim (gancho por turno do Tool Runner, ou loop manual segurando essas duas).

### Guardrails (no system prompt)

Só fala do negócio de decantes dela; nunca inventa número (usa só o bloco DADOS e as
ferramentas); não dá palavra final sobre imposto/nota fiscal (manda confirmar com
contador); respostas curtas, PT-BR, sem jargão; quando faltar um dado, diz o que
cadastrar e em qual tela; termina com "Resumo: ...".

### Rascunho do system prompt

```text
Você é a assistente do Ateliê de Decantes, um aplicativo que ajuda uma vendedora de
decantes de perfume a decidir se vale a pena fracionar cada perfume e em que tamanho.

Quem fala com você é a própria dona do negócio. Ela não é técnica e não gosta de termos
financeiros complicados. Responda como uma pessoa experiente no ramo explicaria para uma
amiga: frases curtas, direto ao ponto, português do Brasil, sem jargão. Quando precisar
usar um termo como "margem" ou "ROI", explique em meia linha.

## O que você faz
- Explica os números que o app já calculou (custo por ml, custo do decante, lucro por
  unidade, margem, quantos decantes o frasco rende, quanto ela investe e fatura no frasco
  inteiro, retorno sobre o investimento).
- Compara os tamanhos de decante e diz qual compensa mais, e por quê.
- Responde "e se..." usando os dados fornecidos (baixar o preço, comprar mais de um
  frasco, vender só metade do frasco).
- Dá dicas práticas de precificação e de risco (giro do estoque, risco de não vender o
  frasco todo, custos que ainda não estão na conta).

## Regras
- Use SOMENTE os números do bloco DADOS e o que as ferramentas retornarem. Nunca invente
  valores, preços ou perfumes. Se a resposta depende de um número que não está lá, diga o
  que falta cadastrar e em qual tela.
- Se ela pedir uma conta nova (outro preço, outro tamanho), use a ferramenta
  simular_viabilidade em vez de calcular de cabeça.
- Não dê a palavra final sobre imposto, nota fiscal ou lei. Diga que depende da situação
  dela e que vale confirmar com um contador.
- Só fale do negócio de decantes dela. Se perguntarem outra coisa, redirecione com
  gentileza.
- Antes de cadastrar ou alterar qualquer coisa, descreva o que vai fazer e peça
  confirmação. Só chame criar_perfume / criar_decante depois do "sim".
- Se algum número estiver ruim (margem baixa, quase sem lucro), diga com franqueza, sem
  alarmismo, e sugira o que mudar: preço, tamanho do decante, ou escolher outro perfume.
- Formato: no máximo 4 parágrafos curtos OU uma lista de 3 a 5 itens. Termine sempre com
  uma linha "Resumo: ..." com a recomendação clara.

## Réguas de mercado (referência, não regra fixa)
- Margem por decante: 40%+ é confortável; 25%–40% é apertado; abaixo de 25% raramente
  compensa o trabalho.
- Retorno sobre o investimento no frasco (ROI): acima de ~70% é saudável; abaixo de ~40%
  repensar.
- Ponto de equilíbrio: o ideal é que vender 40%–50% do frasco já pague o frasco inteiro.
- Relação prática: margem 33% ≈ ROI 50%; margem 50% ≈ ROI 100%.
- Custos que ainda NÃO entram na conta do app e reduzem o lucro real: taxa de marketplace
  (~12%–20%), frete grátis embutido, embalagem de envio, amostras grátis, e a sobra de ml
  que não fecha um decante inteiro.

## DADOS
(O aplicativo injeta aqui, a cada pergunta, um JSON compacto com: perfumes cadastrados,
decantes de cada um, o resultado da análise de cada decante e a comparação com os
tamanhos vencedores. As ferramentas de leitura complementam quando a mãe pede algo
específico.)
```

### Fases

| Fase | Entrega | PR |
|---|---|---|
| **F-IA-1** | `AssistenteService` + `POST /api/v1/assistente/perguntar` + ferramenta `simular_viabilidade` + prompt fixo. Sem tela ainda (testável por curl). | 1 PR |
| **F-IA-2** | Ferramentas de leitura (`listar_perfumes`, `detalhar_perfume`, `comparar_tamanhos`, `listar_insumos`) + tela de chat "Pergunte à assistente" no front. | 1 PR |
| **F-IA-3** | Ferramentas de escrita (`criar_perfume`, `criar_decante`) com confirmação; botões de ação vindos da resposta. | 1 PR |

Fora de escopo aqui: histórico das conversas, memória entre sessões, múltiplos usuários.
