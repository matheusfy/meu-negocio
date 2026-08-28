# Planejamento — Sistema de Viabilidade para Venda de Decantes

## 1. Objetivo

Criar um sistema simples para cadastrar perfumes/produtos, informar seus custos e simular a produção de decantes de diferentes tamanhos.

O sistema deve permitir descobrir:

- Quanto custa produzir cada decante.
- Quantos decantes podem ser produzidos.
- Quanto será investido.
- Receita estimada.
- Lucro bruto estimado.
- Margem de lucro.
- Qual tamanho de decante apresenta melhor viabilidade.

> **Observação:** inicialmente o cálculo será de lucro bruto. Custos adicionais como impostos, taxas de marketplace, frete e comissões podem ser adicionados posteriormente para chegar ao lucro líquido.

---

## 2. Conceito principal

O produto principal será o perfume.

Exemplo:

- Perfume: R$ 300,00
- Volume: 100 ml

A partir dele serão criados diferentes cenários de venda:

- Decante de 3 ml
- Decante de 5 ml
- Decante de 10 ml

Cada tamanho terá seus próprios custos de embalagem e preço de venda.

---

## 3. Cadastro do Produto

### Entidade `Produto`

```java
private Long id;
private String nome;
private String marca;
private String categoria;
private String sku;
private Long fornecedorPrincipalId;

private BigDecimal precoVenda;
private BigDecimal precoCusto;

private BigDecimal volumeMl;

private int estoqueAtual;
private int estoqueMinimo;

private int userId = 1;
private boolean ativo;
```

### Observação

`precoVenda` pode representar o preço de venda do produto original, caso futuramente exista também a possibilidade de venda do perfume inteiro.

Para a simulação de decantes, o principal valor será:

- `precoCusto`
- `volumeMl`

---

## 4. Cadastro dos custos de produção

Os custos necessários para produzir um decante podem ser separados.

### Custos iniciais

- Frasco/vasilha
- Seringa
- Etiqueta

Futuramente:

- Tampa
- Lacre
- Caixa
- Embalagem para envio
- Mão de obra
- Taxas
- Outros custos

### Entidade `CustoInsumo`

```java
private Long id;
private String nome;
private BigDecimal custoUnitario;
private String unidade;
private boolean ativo;
```

Exemplos:

| Insumo | Custo |
|---|---:|
| Seringa | R$ 0,50 |
| Frasco 3 ml | R$ 1,50 |
| Frasco 5 ml | R$ 2,00 |
| Frasco 10 ml | R$ 3,50 |
| Etiqueta | R$ 0,20 |

---

## 5. Configuração do Decante

O decante representa uma forma de transformar parte do perfume em uma unidade de venda.

### Entidade `Decante`

```java
private Long id;
private Long produtoId;

private BigDecimal volumeMl;
private BigDecimal precoVenda;

private BigDecimal custoEmbalagem;
private BigDecimal custoSeringa;
private BigDecimal custoEtiqueta;

private boolean ativo;
```

Exemplo:

### Decante de 5 ml

```text
Produto: Perfume A
Volume: 5 ml
Preço de venda: R$ 30,00

Frasco: R$ 2,00
Seringa: R$ 0,50
Etiqueta: R$ 0,20
```

---

# 6. Cálculo do custo do perfume

O primeiro cálculo será descobrir quanto custa cada ml do perfume.

```text
Custo por ml = Preço de custo do perfume / Volume total
```

Exemplo:

```text
Perfume: R$ 300,00
Volume: 100 ml

R$ 300 / 100 = R$ 3,00 por ml
```

---

# 7. Custo do perfume dentro do decante

```text
Custo do perfume no decante =
Custo por ml × Volume do decante
```

Exemplo:

```text
R$ 3,00 × 5 ml = R$ 15,00
```

---

# 8. Custo total do decante

```text
Custo total =
Custo do perfume
+ Frasco
+ Seringa
+ Etiqueta
+ Outros custos
```

Exemplo:

```text
Perfume:  R$ 15,00
Frasco:   R$  2,00
Seringa:  R$  0,50
Etiqueta: R$  0,20

Custo total: R$ 17,70
```

---

# 9. Lucro por decante

```text
Lucro = Preço de venda - Custo total
```

Exemplo:

```text
Venda: R$ 30,00
Custo: R$ 17,70

Lucro: R$ 12,30
```

---

# 10. Margem de lucro

```text
Margem (%) = (Lucro / Preço de venda) × 100
```

Exemplo:

```text
R$ 12,30 / R$ 30,00 × 100

Margem = 41%
```

---

# 11. Quantidade de decantes

A quantidade teórica será:

```text
Quantidade = Volume do perfume / Volume do decante
```

Exemplo:

```text
Perfume: 100 ml
Decante: 5 ml

100 / 5 = 20 decantes
```

Porém, o sistema deve futuramente permitir considerar uma **perda de produção**.

Exemplo:

```text
Perda estimada: 5%

Volume aproveitável = 100 × 0,95
Volume aproveitável = 95 ml

95 / 5 = 19 decantes
```

Isso deixa a análise mais realista.

---

# 12. Viabilidade do lote

A análise deve considerar o perfume inteiro como um investimento.

Exemplo:

```text
Custo do perfume: R$ 300,00

20 decantes × R$ 30,00
Receita potencial: R$ 600,00
```

Custos:

```text
Perfume:       R$ 300,00
Frascos:       R$  40,00
Seringas:      R$  10,00
Etiquetas:     R$   4,00

Custo total:   R$ 354,00
```

Resultado:

```text
Receita:       R$ 600,00
Custo total:   R$ 354,00
Lucro:         R$ 246,00
Margem:        41%
```

---

# 13. Comparação entre tamanhos

Um dos principais recursos do sistema será comparar diferentes tamanhos de decantes.

Exemplo:

| Tamanho | Custo | Venda | Lucro | Margem |
|---|---:|---:|---:|---:|
| 3 ml | R$ 11,20 | R$ 20,00 | R$ 8,80 | 44% |
| 5 ml | R$ 17,70 | R$ 30,00 | R$ 12,30 | 41% |
| 10 ml | R$ 33,50 | R$ 50,00 | R$ 16,50 | 33% |

O sistema poderá indicar:

```text
Melhor margem: 3 ml
Maior lucro por unidade: 10 ml
Melhor retorno do investimento: 3 ml
```

---

# 14. Dashboard da simulação

A tela principal da análise poderia apresentar:

```text
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 PERFUME A
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 Custo:              R$ 300,00
 Volume:             100 ml

 Receita estimada:   R$ 600,00
 Custo total:        R$ 354,00
 Lucro estimado:     R$ 246,00
 Margem:             41%
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

E abaixo:

```text
Cenários de venda

3 ml   → Margem: 44%
5 ml   → Margem: 41%
10 ml  → Margem: 33%
```

---

# 15. Arquitetura inicial

Como o objetivo é manter a aplicação simples, a arquitetura pode seguir:

```text
controller
    ↓
service
    ↓
repository
    ↓
database
```

Entidades principais:

```text
Produto
CustoInsumo
Decante
```

Serviços:

```text
ProdutoService
CustoInsumoService
DecanteService
AnaliseViabilidadeService
```

O `AnaliseViabilidadeService` será responsável pelos cálculos e não deve armazenar os resultados calculados diretamente no `Produto`.

---

# 16. Responsabilidade do `AnaliseViabilidadeService`

O serviço poderá receber um produto e uma configuração de decante e retornar uma estrutura de análise.

Exemplo conceitual:

```java
AnaliseViabilidade analisar(
    Produto produto,
    Decante decante
);
```

Resultado:

```java
private BigDecimal custoPorMl;
private BigDecimal custoProduto;
private BigDecimal custoEmbalagem;
private BigDecimal custoTotal;
private BigDecimal precoVenda;
private BigDecimal lucro;
private BigDecimal margem;
private int quantidadeDecantes;
private BigDecimal receitaTotal;
private BigDecimal lucroTotal;
```

---

# 17. Evolução futura

Depois do MVP, podem ser adicionados:

## Custos

- Impostos
- Taxas de marketplace
- Comissão
- Frete
- Embalagem de envio
- Mão de obra
- Perdas

## Estoque

- Controle de estoque do perfume
- Controle de frascos
- Controle de seringas
- Controle de etiquetas
- Baixa automática dos insumos

## Análise financeira

- ROI
- Ponto de equilíbrio
- Lucro mensal
- Faturamento mensal
- Custo médio
- Ticket médio
- Giro de estoque

## Histórico

Registrar cada venda para comparar:

```text
Estimativa × Resultado real
```

---

# 18. MVP recomendado

Para a primeira versão, implementar somente:

### Cadastro

- [ ] Produto
- [ ] Custo do produto
- [ ] Volume do produto
- [ ] Insumos
- [ ] Decantes
- [ ] Preço de venda

### Cálculos

- [ ] Custo por ml
- [ ] Custo do produto no decante
- [ ] Custo total do decante
- [ ] Quantidade de decantes
- [ ] Receita estimada
- [ ] Lucro por unidade
- [ ] Lucro total
- [ ] Margem

### Análise

- [ ] Comparação entre 3 ml, 5 ml e 10 ml
- [ ] Identificação da maior margem
- [ ] Identificação do maior lucro por unidade
- [ ] Identificação do melhor cenário

---

# 19. Objetivo final

O sistema deve responder de forma simples:

> **"Tenho R$ X investidos nesse perfume. Se eu transformar esse produto em decantes de 3, 5 ou 10 ml, quanto vou gastar, quanto posso faturar e quanto vou ganhar?"**

Essa será a principal proposta de valor do sistema.

A partir disso, a aplicação deixa de ser apenas um cadastro de produtos e passa a ser uma ferramenta para **simular e tomar decisões de compra e venda com base nos custos reais**.
