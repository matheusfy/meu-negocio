# Planejamento — Área de Vendas + Estoque com custo médio ("Ateliê de Decantes")

> Objetivo: a mãe registra **as compras dos frascos** (em datas e preços diferentes) e
> **as vendas** (decante ou vidro cheio). O sistema mantém o **custo médio ponderado**
> do frasco de cada perfume, sabe **quantos frascos estão lacrados e quantos estão abertos**
> (para dizer se dá ou não para vender um vidro cheio), dá baixa no estoque a cada venda e
> mostra **faturamento e lucro até o momento** + uma **previsão de lucro** do estoque restante.
>
> Continua sem login, sem impostos, sem despesas fixas (ver §9). Tudo em português, para a usuária final.

---

## 0. A ideia em um desenho

### Ciclo de vida de um frasco

```
   COMPRA                        "abrir frasco"                 ml acabou
(LoteCompra)                     (ação da mãe)
    │                                 │                             │
    ▼                                 ▼                             ▼
┌───────────────┐  venda        ┌───────────────┐  venda      ┌───────────┐
│    FRASCO     │  VIDRO_CHEIO  │    FRASCO      │  DECANTE    │  FRASCO   │
│    LACRADO    │ ────────────▶ │    ABERTO      │ ──────────▶ │  ESGOTADO │
│ (em estoque)  │   (sai do     │ (100 ml → …ml) │  (tira 5,   │           │
└───────────────┘    estoque)   └───────────────┘   10 ml…)    └───────────┘
        │                              ▲
        └──────────────────────────────┘
        só um frasco LACRADO pode virar aberto,
        e só um LACRADO pode ser vendido cheio
```

### O estoque de um perfume, a qualquer momento

```
frascos comprados (soma dos lotes) ......... 4
  − vendidos cheios ....................... 1
  − abertos para decante ................. 1
  ────────────────────────────────────────────
  = FRASCOS LACRADOS  ..................... 2   →  "pode vender um vidro cheio? SIM (2)"

frascos abertos: 1   com  75 ml restantes   →  ≈ 15 decantes de 5 ml ainda saem daqui
```

### O dinheiro

```
custo médio do frasco = tudo que paguei em frascos ÷ nº de frascos comprados
                      = R$ 1.120 ÷ 4 = R$ 280,00 por frasco   (= R$ 2,80 / ml num frasco de 100 ml)

venda de DECANTE 5 ml por R$ 50,00:
    custo  = 2,80 × 5  +  embalagem 7,80  =  R$ 21,80
    lucro  = 50,00 − 21,80  =  R$ 28,20        ◀ congela nessa venda pra sempre

venda de VIDRO CHEIO por R$ 350,00:
    custo  = R$ 280,00 (o custo médio de hoje)
    lucro  = 350,00 − 280,00  =  R$ 70,00      ◀ congela nessa venda pra sempre

Se amanhã eu comprar um frasco mais caro, o custo médio sobe —
mas essas duas vendas continuam com o lucro que já foi congelado.
```

---

## 1. O que muda no que já existe

| Hoje | Depois |
|---|---|
| `Produto.precoCusto` digitado na mão no cadastro | Vira **calculado** — média ponderada das compras. Só editável enquanto não houver compra lançada. |
| `Produto.estoqueAtual` (int, sem uso real) | Deixa de ser digitado. Saldo = **derivado**: `frascos comprados − vendidos cheios − abertos` (lacrados) + ml restante nos abertos. |
| `Produto.estoqueMinimo` (int, sem uso) | Passa a valer: alerta "abaixo do mínimo" (em frascos lacrados) no Início. |
| `Decante` = configuração de venda por tamanho | Sem mudança. Uma **venda de decante** aponta para o `Decante` (tamanho + embalagem) e para o **frasco aberto** de onde saiu. |
| `AnaliseViabilidadeService` (simulação "e se") | Sem mudança de assinatura. A venda real reaproveita a **mesma fórmula de custo**. |

Nenhuma migration destrutiva: só tabelas novas e o serviço passa a recalcular `produtos.preco_custo`.

---

## 2. Modelo de domínio — entidades novas

### 2.1 `LoteCompra` — tabela `lotes_compra`

Uma entrada de estoque: "comprei N frascos do perfume X no dia D a R$ P cada".

| Campo | Tipo | Regra |
|---|---|---|
| `id` | BIGINT IDENTITY | |
| `produtoId` | BIGINT · FK → `produtos` | obrigatório |
| `dataCompra` | DATE | obrigatório (default: hoje) |
| `quantidadeFrascos` | INT | > 0 |
| `precoUnitario` | NUMERIC(12,2) | ≥ 0 — preço pago **por frasco** |
| `custoAdicional` | NUMERIC(12,2) | opcional, default 0 — frete/taxas do pedido, **rateado no lote** |
| `fornecedor` | VARCHAR(255) | opcional, texto livre |
| `observacao` | VARCHAR(500) | opcional |
| `userId`, `ativo`, auditoria | | padrão do projeto |

**Custo efetivo do frasco neste lote:** `precoUnitario + custoAdicional / quantidadeFrascos`

### 2.2 `FrascoAberto` — tabela `frascos_abertos`

Um frasco que a mãe **abriu para decantar**. Enquanto existe, esse frasco **não pode mais ser
vendido cheio**. Serve só para **controlar o estoque** (quantos ml ainda dão para decantar);
o custo não fica aqui — ele é resolvido em cada venda (§3.3).

| Campo | Tipo | Regra |
|---|---|---|
| `id` | BIGINT IDENTITY | |
| `produtoId` | BIGINT · FK → `produtos` | obrigatório |
| `dataAbertura` | DATE | obrigatório (default: hoje) |
| `volumeInicialMl` | NUMERIC(10,2) | **congelado**: `produto.volumeMl` na hora da abertura |
| `mlRestante` | NUMERIC(10,2) | começa igual a `volumeInicialMl`; decresce a cada venda de decante |
| `status` | VARCHAR(15) | `ABERTO` \| `ESGOTADO` \| `DESCARTADO` |
| `observacao` | VARCHAR(500) | opcional |
| `userId`, auditoria | | padrão do projeto |

Abrir um frasco exige **≥ 1 frasco lacrado** em estoque (§3.2).

### 2.3 `Venda` — tabela `vendas`

Uma venda realizada. **Guarda os valores congelados** no momento da venda — histórico não muda depois.

| Campo | Tipo | Regra |
|---|---|---|
| `id` | BIGINT IDENTITY | |
| `produtoId` | BIGINT · FK → `produtos` | obrigatório |
| `dataVenda` | DATE | obrigatório |
| `tipo` | VARCHAR(20) | `VIDRO_CHEIO` \| `DECANTE` |
| `decanteId` | BIGINT · FK → `decantes` | obrigatório se `tipo = DECANTE` |
| `frascoAbertoId` | BIGINT · FK → `frascos_abertos` | obrigatório se `tipo = DECANTE` — de qual frasco saiu |
| `quantidade` | INT | > 0 (nº de frascos cheios, ou nº de decantes) |
| `volumeUnitarioMl` | NUMERIC(10,2) | **congelado**: volume do frasco ou do decante |
| `precoUnitario` | NUMERIC(12,2) | preço realmente cobrado por unidade (a mãe pode dar desconto) |
| `custoUnitario` | NUMERIC(12,2) | **snapshot** — custo médio do frasco na hora da venda (§3.3) |
| `receitaTotal` | NUMERIC(12,2) | `= precoUnitario × quantidade` |
| `lucroTotal` | NUMERIC(12,2) | `= (precoUnitario − custoUnitario) × quantidade` |
| `formaPagamento` | VARCHAR(30) | opcional |
| `observacao` | VARCHAR(500) | opcional |
| `userId`, `ativo`, auditoria | | padrão do projeto |

`receitaTotal`, `custoUnitario` e `lucroTotal` são **colunas gravadas**. Se depois entrar um lote
mais caro/barato, **esta venda continua igual** (princípios #5 e #7 do
`planejamento_sistema_gestao_rentabilidade.md`).

### 2.4 Custo do perfume (`produtos.preco_custo`)

Vira um **cache do custo médio**, recalculado pelo `EstoqueService` a cada mudança de `LoteCompra`.
O `AnaliseViabilidadeService` e a tela de Viabilidade continuam **sem alteração** — só passam a ler
um número vindo das compras. Sem compra lançada → a mãe ainda pode digitar (seed / compra antiga).

---

## 3. Regras de cálculo

Mesmas escalas do `AnaliseViabilidadeService`: custo/ml com **4 casas**, dinheiro com **2 casas**,
`RoundingMode.HALF_UP`, tudo em `BigDecimal`.

### 3.1 Custo médio ponderado do frasco  — *decisão confirmada: média de TODOS os lotes*

```
                 Σ ( lote.quantidadeFrascos × custoEfetivoFrasco(lote) )
custoMedioFrasco = ──────────────────────────────────────────────────────
                            Σ lote.quantidadeFrascos

custoMedioMl = custoMedioFrasco ÷ produto.volumeMl        (4 casas)
```
É o "valor mínimo de despesa do frasco" que a mãe usa para saber o lucro. Uma linha por perfume.

### 3.2 Saldo de estoque — lacrados, abertos e ml

```
frascosComprados      = Σ lote.quantidadeFrascos
frascosVendidosCheios = Σ venda[VIDRO_CHEIO].quantidade
frascosAbertosTotais  = count(FrascoAberto)                       -- inclui ESGOTADO/DESCARTADO

frascosLacrados = frascosComprados − frascosVendidosCheios − frascosAbertosTotais
                  ▲ "posso vender um vidro cheio?"  →  frascosLacrados ≥ quantidade pedida

mlNosAbertos = Σ frascoAberto.mlRestante        (status = ABERTO)
decantesPossiveis(tam) = floor(mlNosAbertos ÷ tam)

valorEstoque = frascosLacrados × custoMedioFrasco  +  mlNosAbertos × custoMedioMl
```

- **Vender vidro cheio** → só consome `frascosLacrados`.
- **Vender decante** → só consome `mlRestante` de um `FrascoAberto` (§3.4). Nunca "quebra" um lacrado sozinho.
- **Abrir frasco** → `frascosLacrados` − 1, cria um `FrascoAberto`.

### 3.3 Custo unitário de cada venda (congelado na hora da venda)

O custo sempre parte do **custo médio do frasco de hoje** (§3.1). O `FrascoAberto` **não** guarda
custo — ele só diz de qual estoque de ml a venda saiu.

| `tipo` | `custoUnitario` gravado na venda |
|---|---|
| `VIDRO_CHEIO` | `custoMedioFrasco` (do momento da venda) |
| `DECANTE` | `custoMedioMl × decante.volumeMl + decante.custoEmbalagemTotal()` (do momento da venda) |

A linha do decante é a **mesma fórmula** do `AnaliseViabilidadeService.analisar()`, trocando
`produto.precoCusto` pelo `custoMedioMl`. Depois:
```
receitaTotal = precoUnitario × quantidade
lucroTotal   = (precoUnitario − custoUnitario) × quantidade
```
Uma vez gravado, **nunca recalcula** — nem se o custo médio mudar depois.

### 3.4 Venda de decante: de qual frasco aberto sai

Auto-seleção **FIFO**: o `FrascoAberto` mais antigo (`dataAbertura`, depois `id`) com
`mlRestante ≥ quantidade × decante.volumeMl`.

- Nenhum frasco aberto atende → a tela oferece **"abrir um frasco agora"** (precisa de lacrado).
- **Sem frasco aberto e sem lacrado para abrir → a venda é bloqueada** (§8, decisão confirmada).
- Ao gravar: `frascoAberto.mlRestante −= quantidade × volume`; se zerar, `status = ESGOTADO`.
- (Backlog) dividir uma venda entre dois frascos abertos.

### 3.5 Resultado "até o momento" (`ResultadoService`)

Filtro de período opcional (`de`, `ate`); "até o momento" = sem filtro final.
```
faturamento = Σ venda.receitaTotal
custo (CMV) = Σ ( venda.custoUnitario × venda.quantidade )
lucroBruto  = faturamento − custo            ( = Σ venda.lucroTotal )
nºVendas    = count(vendas)
ticketMedio = faturamento ÷ nºVendas
margemMedia = lucroBruto ÷ faturamento × 100
```
Os mesmos números **agrupados por perfume** → ranking "mais lucrativos".

### 3.6 Previsão de lucro do estoque restante

```
Para cada perfume:
  se tem decante ativo → tam = tamanho de melhor margem (vem do ComparacaoDecantes)
     lucroAbertos  = floor(mlNosAbertos ÷ tam)                        × lucroUnitárioEstimado(tam)
     lucroLacrados = frascosLacrados × floor(volumeMl ÷ tam)          × lucroUnitárioEstimado(tam)
  senão, se tem preço de venda do vidro:
     lucroLacrados = frascosLacrados × (precoVendaVidro − custoMedioFrasco)
     lucroAbertos  = 0
lucroPrevistoTotal = Σ (lucroAbertos + lucroLacrados)
```
`lucroUnitárioEstimado` usa o `custoMedioMl` de hoje. Frase no Início:
*"Vendendo todo o estoque de hoje do jeito mais rentável, você lucra ≈ R$ X."*

### 3.7 Exemplo numérico (perfume 100 ml · decante 5 ml · embalagem R$ 7,80)

```
Compra 1 (10/03):  2 frascos × R$ 280,00      = R$ 560,00
Compra 2 (02/06):  1 frasco  × R$ 310,00      = R$ 310,00
                   ────────────────────────────────────────
                   3 frascos, R$ 870,00  →  custo médio R$ 290,00/frasco  →  R$ 2,90/ml

Estoque: 3 lacrados, 0 abertos.  "Pode vender cheio? SIM (3)."

Abrir 1 frasco (12/06):
  FrascoAberto { volumeInicialMl 100, mlRestante 100, status ABERTO }
  Estoque: 2 lacrados, 1 aberto (100 ml).

Venda DECANTE 5 ml a R$ 50,00 (15/06):
  custoUnitario = 2,90 × 5 + 7,80 = R$ 22,30      (congela; custo médio era R$ 290)
  lucroTotal    = 50,00 − 22,30   = R$ 27,70
  frascoAberto.mlRestante = 95

Compra 3 (20/07): 1 frasco × R$ 250,00
  novo custo médio = (560 + 310 + 250) / 4 = R$ 280,00/frasco  →  R$ 2,80/ml
  → a venda de 15/06 CONTINUA lucro R$ 27,70 (congelado)
  → vendas NOVAS de decante passam a custar 2,80 × 5 + 7,80 = R$ 21,80
  → vendas NOVAS de vidro cheio passam a custar R$ 280,00

Estoque: 3 lacrados + 1 aberto (95 ml).  Valor ≈ 3×280 + 95×2,80 = R$ 1.106,00
```

---

## 4. API REST (mesmo padrão dos controllers atuais)

```
POST   /api/v1/lotes                     cria compra → recalcula custo médio do produto
GET    /api/v1/lotes?produtoId=          lista (Page<>)
PUT    /api/v1/lotes/{id}   DELETE ...    edita/remove → recalcula (não toca vendas passadas)

POST   /api/v1/frascos-abertos           abre um frasco (body: produtoId) → exige frasco lacrado
GET    /api/v1/frascos-abertos?produtoId=&status=
DELETE /api/v1/frascos-abertos/{id}      marca DESCARTADO (não volta a ser lacrado)

POST   /api/v1/vendas                    calcula e CONGELA custo/lucro; baixa lacrado (cheio) ou ml (decante, FIFO)
GET    /api/v1/vendas?produtoId=&de=&ate=&tipo=
PUT    /api/v1/vendas/{id}   DELETE ...   editar recongela; excluir estorna a baixa

GET    /api/v1/produtos/{id}/estoque
       → { frascosLacrados, frascosAbertos, mlNosAbertos, custoMedioFrasco, custoMedioMl,
           valorEstoque, estoqueMinimo, abaixoDoMinimo, podeVenderCheio }

GET    /api/v1/resultado?de=&ate=
       → { faturamento, custo, lucroBruto, margemMedia, numVendas, ticketMedio,
           porPerfume:[…], lucroPrevistoEstoque }
```

- `ResponseEntity`, DTO `@Validated`, `201 + Location` no POST — igual a `ProdutoController` / `DecanteController`.
- `GlobalExceptionHandler` já cobre 400 / 404 / 409 / 500; o front já consome `ErroResposta`.
- **Venda sem estoque (cheio sem lacrado, ou decante sem ml e sem lacrado para abrir) → 409, bloqueada.**

---

## 5. Migrations (Flyway, append-only)

| Arquivo | Conteúdo |
|---|---|
| `V6__create_lotes_compra_table.sql` | `lotes_compra` — IDENTITY, `user_id` default 1, auditoria, índices `produto_id` / `user_id` / `data_compra`, FK `fk_lotes_compra_produto` |
| `V7__create_frascos_abertos_table.sql` | `frascos_abertos` — idem + índice `produto_id` / `status`, FK `fk_frascos_abertos_produto` |
| `V8__create_vendas_table.sql` | `vendas` — idem + índice `data_venda`, FKs `fk_vendas_produto`, `fk_vendas_decante` (nullable), `fk_vendas_frasco_aberto` (nullable) |

Sem migration para `produtos.preco_custo` (schema igual; só o serviço passa a escrever).
Não mexem nas V1–V5. Se o Postgres de dev travar no Flyway, `docker compose down -v`
(ver `planejamento_acesso_rede_local.md`).

---

## 6. Front-end (front estático — `static/app.js`, `index.html`)

| Tela | Rota | Conteúdo |
|---|---|---|
| **Estoque** | `estoque` | Card por perfume: **frascos lacrados** (selo "pode vender cheio" / "sem lacrado"), **frascos abertos** (ml restante ≈ N decantes), custo médio, valor em estoque, aviso ⚠ abaixo do mínimo. Form **"Registrar compra"** (data, quantidade, preço/frasco, frete opcional). Botão **"Abrir um frasco"**. Tabela de lotes (editar/excluir) e de frascos abertos (descartar). |
| **Vendas** | `vendas` | Form **"Registrar venda"**: perfume → tipo. **Vidro cheio**: mostra `frascosLacrados`, bloqueia se 0. **Decante**: mostra frascos abertos e ml; se não houver, oferece "abrir um frasco agora"; escolhe tamanho (do `Decante`) → quantidade → preço cobrado → data. Tabela de vendas com filtro de período e **totais no rodapé**. |
| **Início** | `inicio` | Tiles: Faturamento do mês · Lucro do mês · Ticket médio · Nº de vendas. Bloco "Lucro previsto do estoque". Lista "perfumes abaixo do mínimo". |
| **Perfume / detalhe** | `perfume` | Linha "Estoque: X lacrados · Y abertos (Z ml) · custo médio R$ W" e "Vendidos: N · lucro R$ V". Campo **"preço de custo" do form de editar vira somente-leitura** quando já há compras. |

- `index.html`: botões `estoque` e `vendas`; ícones em `IC`; entradas em `NAV_FOR` e `RENDERERS`.
- Conta financeira **só no back** (`BigDecimal`); o front formata com `brl()`.
- Mesma identidade visual e mesmo tom ("quanto você lucrou", não "lucroTotal").

---

## 7. Fases de entrega (1 branch `issue-N-…` → build local → PR → squash-merge)

| Fase | Entrega | PR |
|---|---|---|
| **1 — Estoque** | `LoteCompra` + `FrascoAberto` + `V6`/`V7` + CRUD `/lotes` + `POST/GET/DELETE /frascos-abertos` + `EstoqueService` (custo médio, lacrados/abertos/ml) + `GET /produtos/{id}/estoque` + `preco_custo` calculado + tela **Estoque** + saldo no detalhe do perfume + `EstoqueServiceTest` | 1 |
| **2 — Vendas de vidro cheio** | `Venda` + `V8` + CRUD `/vendas` (`VIDRO_CHEIO`) com snapshot + baixa de lacrados + `ResultadoService` + `GET /resultado` + tela **Vendas** + tiles no Início + `ResultadoServiceTest` | 1 |
| **3 — Vendas de decante** | `tipo = DECANTE` ligado a `Decante` + `FrascoAberto` (FIFO), custo pela fórmula da viabilidade com custo médio, baixa em ml, "abrir frasco" inline na tela de venda | 1 |
| **4 — Previsão & alertas** | Lucro previsto do estoque + alerta de estoque mínimo + filtro de período no dashboard + ranking "mais lucrativos" + ajuste/descarte de estoque (perda/quebra, sobra do frasco aberto) | 1 |
| **Backlog** | Despesas fixas · ponto de equilíbrio · taxa de cartão/marketplace/comissão · clientes · fornecedores como entidade · devoluções · vender a sobra de um frasco aberto com desconto · custeio FIFO por lote | — |

---

## 8. Decisões

| # | Decisão | Status |
|---|---|---|
| 1 | **Frasco lacrado × aberto** — entidade `FrascoAberto`; lacrados (vendáveis cheios) contados à parte dos ml em frascos abertos (só decante); abrir frasco é ação explícita | ✅ confirmado |
| 2 | **Custo** — média ponderada de **todos os lotes** do perfume (§3.1); congelada em cada venda; sem exceção por frasco | ✅ confirmado |
| 3 | **Venda sem estoque** — **não é permitida** (erro 409): vidro cheio exige lacrado; decante exige ml em frasco aberto (ou um lacrado para abrir na hora) | ✅ confirmado |
| 4 | **`produtos.preco_custo` calculado** — deixa de ser digitado; passa a ser a média das compras, recalculada automaticamente; o campo no cadastro fica **somente-leitura** quando já existe alguma compra lançada (editável só enquanto não houver nenhuma) | ✅ confirmado |
| 5 | **Nomes internos** (não aparecem para a mãe) — `LoteCompra`/`lotes_compra`, `FrascoAberto`/`frascos_abertos`, `Venda`/`vendas` | ✅ confirmado |

---

## 9. Fora de escopo agora

Despesas fixas · ponto de equilíbrio · taxa de cartão/marketplace/comissão · frete rateado por item ·
clientes e fornecedores como entidades · devoluções · metas mensais · login/multiusuário ·
custeio FIFO por lote · relatórios exportáveis.

Quando entrar despesas fixas + ponto de equilíbrio, vira a **Fase 5** e fecha a "Fase E" do `NEXT_STEPS.md`.

---

## 10. Riscos / atenção

- **Imutabilidade do histórico** (princípios #5 e #7): `Venda` guarda `custoUnitario` / `lucroTotal`
  congelados; **nunca** recalcular quando entra lote novo. Cobrir com teste.
- **Editar/excluir lote depois de vender** muda o custo médio **atual** e as previsões — **não pode**
  mexer em vendas já feitas. A tela avisa isso ao editar um lote.
- **Abrir frasco** consome um lacrado de forma irreversível (só "descartar" depois, não "relacrar").
- **Sobra do frasco aberto** (ml que não viram decante inteiro / evaporação) — ajuste manual na Fase 4.
- **Sem lote lançado** → cair no `preco_custo` digitado como fallback (seed / compra antiga).
- **`estoqueAtual` legado** (int em `produtos`) — ignorar a coluna por ora; dropar em migration futura.
- **CI sensível / GitGuardian / `.claude/settings.local.json` sempre `M`** — igual aos outros planos.
- **Spring Boot 4 SNAPSHOT** — usar só API estável de JPA / `WebMvcConfigurer`.
- **Atualizar `NEXT_STEPS.md`** ao fim da Fase 2 (parte da "Fase E" entregue).
</content>
