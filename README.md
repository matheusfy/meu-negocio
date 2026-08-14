# MeuNegocio

Sistema de gestão e rentabilidade para pequenos negócios. Nasce para resolver um problema real — controlar produtos, compras, estoque, vendas e financeiro das vendas de perfume da família — e é desenhado para responder perguntas que uma planilha não responde bem:

- Quanto estou faturando? Quanto estou gastando? Quanto estou lucrando de verdade?
- Qual a margem de lucro de cada produto? Quais produtos são mais lucrativos?
- Quanto preciso vender para cobrir minhas despesas?
- Quais produtos precisam ser repostos?

O plano detalhado (módulos, regras de negócio, exemplos de cálculo, roadmap completo) está em [`planejamento_sistema_gestao_rentabilidade.md`](planejamento_sistema_gestao_rentabilidade.md). Este README é o resumo prático: o que o sistema faz, como rodar e em que pé está.

## O que o sistema faz

Centraliza as informações do negócio e transforma dados de compra/venda em decisão, seguindo o fluxo:

```text
Produto → Compra → Estoque → Venda → Resultado Financeiro
```

- **Produtos** — cadastro, com custo tratado como histórico (um produto pode ter sido comprado por preços diferentes ao longo do tempo), não como valor fixo.
- **Compras** — registra cada aquisição (fornecedor, quantidade, frete, outras despesas) e calcula o custo real de colocar o produto no estoque.
- **Estoque** — movimentações de entrada/saída (compra, venda, ajuste, devolução, perda) e indicadores (estoque atual, mínimo, valor total).
- **Vendas** — cada venda usa o custo do produto **no momento em que foi vendido**, não o custo atual — histórico financeiro nunca é reescrito.
- **Custos e despesas** — separa custo variável (ligado ao produto: embalagem, taxa de cartão, frete da venda) de despesa fixa (aluguel, internet, sistemas).
- **Financeiro** — receitas e despesas consolidadas em lucro bruto e lucro líquido.
- **Indicadores de rentabilidade** — margem, markup e rentabilidade por produto e do negócio como um todo; ponto de equilíbrio (quanto preciso faturar pra cobrir as despesas).
- **Dashboard** — visão resumida: faturamento, lucro, produtos mais vendidos/lucrativos, estoque baixo, evolução no tempo.

## Princípios do projeto

1. Começar simples — resolver primeiro uma necessidade real, sem funcionalidade desnecessária no MVP.
2. Nunca sobrescrever histórico financeiro/estoque importante.
3. Separar custo, receita e despesa com clareza.
4. Calcular lucro com base no custo correto da mercadoria no momento da venda.
5. Projetar o domínio pra poder evoluir além do segmento de perfumes, se fizer sentido.
6. Priorizar usabilidade pra quem não tem conhecimento técnico ou contábil.

## Tecnologias utilizadas

**Backend**
- Java 21
- Spring Boot (Web, Data JPA, Validation, Session JDBC)
- Gradle

**Persistência**
- PostgreSQL (dados transacionais)
- H2 (testes)
- Flyway (migrations)

**Testes**
- JUnit 5, Spring Boot Test

**Frontend e infraestrutura** (fora do MVP inicial — decididos depois: React/Angular/Vue pro frontend; Docker, CI/CD, cloud e monitoramento já existem no repositório para o próprio ciclo de desenvolvimento, mas não são o foco do MVP).

## Arquitetura

Simples por enquanto, priorizando facilidade de evolução:

```text
Frontend (a definir)
    ↓
API REST
    ↓
Backend (Spring Boot)
    ↓
PostgreSQL
```

## Como executar o projeto

### Pré-requisitos

- Java 21+
- Docker e Docker Compose (o `spring-boot-docker-compose` sobe o Postgres automaticamente)

### Executando a aplicação

```bash
cd meu-negocio
./gradlew bootRun
```

### Rodando os testes

```bash
cd meu-negocio
./gradlew test
```

### Build completo (build + testes + checkstyle)

```bash
cd meu-negocio
./gradlew build
```

Ver [`CONTRIBUTING.md`](CONTRIBUTING.md) para o fluxo de branches, padrão de commit e de nome de branch.

## Roadmap do MVP

| Fase | Foco |
|------|------|
| 1 | Cadastros — produtos, categorias, fornecedores, clientes |
| 2 | Estoque — entrada/saída, ajustes, estoque mínimo, cálculo de custo |
| 3 | Vendas — itens, desconto, forma de pagamento, baixa automática de estoque, cálculo de lucro |
| 4 | Financeiro — receitas, despesas, custos, taxas, resultado financeiro |
| 5 | Dashboard — faturamento, lucro, margem, ticket médio, produtos mais lucrativos, ponto de equilíbrio |

Roadmap completo e exemplos de cálculo em [`planejamento_sistema_gestao_rentabilidade.md`](planejamento_sistema_gestao_rentabilidade.md).

## Status

Projeto em desenvolvimento ativo. Configuração inicial (Git, CI, padrões de commit/branch) concluída — próximo passo é definir o modelo de domínio (entidades, relacionamentos, regras de custo/venda) e iniciar a Fase 1.
