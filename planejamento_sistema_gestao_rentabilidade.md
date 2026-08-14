# Planejamento — Sistema de Gestão e Rentabilidade para Pequenos Negócios

## 1. Visão do projeto

Desenvolver um sistema para auxiliar inicialmente nas vendas de perfumes da mãe do Matheus, permitindo controlar produtos, compras, estoque, vendas e financeiro, além de calcular automaticamente indicadores de rentabilidade.

A ideia é que o sistema não seja apenas um controle de estoque/vendas, mas uma ferramenta para responder perguntas como:

- Quanto estou faturando?
- Quanto estou gastando?
- Quanto realmente estou lucrando?
- Qual é a margem de lucro de cada produto?
- Quais produtos são mais lucrativos?
- Quanto preciso vender para cobrir minhas despesas?
- Quais produtos precisam ser repostos?

O projeto poderá começar como uma solução para uso próprio e, posteriormente, evoluir para um sistema genérico voltado a pequenos negócios.

---

## 2. Objetivo principal

Centralizar as informações do negócio e transformar os dados de compras e vendas em informações úteis para tomada de decisão.

### Fluxo principal

```text
Produto → Compra → Estoque → Venda → Resultado Financeiro
```

---

## 3. Conceito de funcionamento

### Exemplo

Produto:

- Custo de aquisição: R$ 80,00
- Quantidade comprada: 10 unidades
- Frete da compra: R$ 20,00

O sistema pode calcular:

- Custo dos produtos: R$ 800,00
- Frete rateado: R$ 2,00 por unidade
- Custo real unitário: R$ 82,00

Venda:

- Preço de venda: R$ 130,00
- Custo: R$ 82,00
- Lucro bruto: R$ 48,00
- Margem sobre venda: 36,92%
- Markup: 58,54%

---

# 4. Módulos do sistema

## 4.1 Produtos

Cadastro das mercadorias comercializadas.

### Informações iniciais

- Nome
- Marca
- Categoria
- SKU/código
- Fornecedor principal
- Preço de venda
- Estoque atual
- Estoque mínimo
- Status ativo/inativo

### Observação importante

O custo não deve necessariamente ser tratado como um valor fixo no cadastro do produto.

Um mesmo produto pode ser comprado por preços diferentes:

```text
Compra 1 → R$ 80,00
Compra 2 → R$ 85,00
Compra 3 → R$ 78,00
```

Por isso, o sistema deverá manter histórico das compras e permitir o cálculo do custo médio ou outro método de custo definido posteriormente.

---

# 5. Compras / Entrada de mercadorias

Registrar cada aquisição de produtos.

### Informações

- Fornecedor
- Data da compra
- Produtos
- Quantidades
- Custo unitário
- Descontos
- Frete
- Outras despesas
- Valor total

### Exemplo

```text
10 perfumes × R$ 80,00 = R$ 800,00
Frete = R$ 30,00
Outras taxas = R$ 10,00

Custo total = R$ 840,00
Custo médio unitário = R$ 84,00
```

O sistema deve permitir identificar quanto efetivamente custou colocar o produto no estoque.

---

# 6. Estoque

Controle das movimentações de produtos.

### Entradas

- Compra
- Ajuste positivo
- Devolução de venda

### Saídas

- Venda
- Perda
- Ajuste negativo
- Devolução ao fornecedor

### Indicadores

- Estoque atual
- Estoque mínimo
- Produtos sem estoque
- Produtos próximos do estoque mínimo
- Valor total do estoque

---

# 7. Vendas

Registrar cada venda realizada.

### Informações

- Data
- Cliente (opcional)
- Produtos
- Quantidade
- Preço unitário
- Desconto
- Valor total
- Forma de pagamento
- Taxas
- Observações

### Regra importante

O lucro da venda deve utilizar o custo correspondente ao momento da venda.

Exemplo:

```text
Janeiro
Custo do produto = R$ 80,00

Fevereiro
Venda = R$ 130,00

Março
Novo custo = R$ 100,00
```

A venda realizada em fevereiro deve continuar considerando o custo de R$ 80,00, e não o custo atual de R$ 100,00.

Conceitualmente:

```text
Venda
 ├── Produto
 ├── Quantidade
 ├── Preço de venda
 ├── Desconto
 ├── Custo unitário no momento da venda
 └── Lucro
```

---

# 8. Custos e despesas

Separar os custos diretamente relacionados aos produtos das despesas gerais do negócio.

## 8.1 Custos variáveis

Exemplos:

- Custo do produto
- Embalagem
- Sacola
- Taxa do cartão
- Comissão
- Taxas de marketplace
- Frete relacionado à venda

## 8.2 Despesas fixas

Exemplos:

- Internet
- Telefone
- Aluguel
- Sistemas
- Publicidade
- Outras despesas recorrentes

Essa separação permitirá calcular melhor o lucro real do negócio.

---

# 9. Financeiro

Controle das receitas e despesas.

## Receitas

- Vendas
- Outras entradas

## Despesas

- Compras
- Fretes
- Publicidade
- Taxas
- Despesas operacionais
- Outras despesas

### Resultado

```text
Faturamento
- Custo dos produtos
= Lucro bruto

Lucro bruto
- Despesas
= Lucro líquido
```

---

# 10. Indicadores de rentabilidade

Essa será uma das principais funcionalidades do sistema.

## Por produto

Mostrar:

- Preço de venda
- Custo
- Lucro bruto
- Margem de lucro
- Markup
- Rentabilidade

### Exemplo

```text
Preço de venda: R$ 130,00
Custo: R$ 82,00

Lucro bruto: R$ 48,00
Margem: 36,92%
Markup: 58,54%
```

## Indicadores gerais

- Faturamento
- Custo dos produtos vendidos
- Lucro bruto
- Despesas
- Lucro líquido
- Margem média
- Ticket médio
- Quantidade de vendas
- Quantidade de produtos vendidos

---

# 11. Ponto de equilíbrio

O sistema deverá futuramente calcular quanto o negócio precisa faturar para cobrir suas despesas.

### Exemplo

```text
Despesas fixas mensais: R$ 1.500,00
Margem de contribuição média: 40%

Faturamento necessário:
R$ 1.500 / 0,40 = R$ 3.750,00
```

Resultado apresentado ao usuário:

> Você precisa faturar aproximadamente R$ 3.750,00 por mês para cobrir suas despesas.

Esse indicador ajuda a entender quando o negócio começa efetivamente a gerar lucro.

---

# 12. Dashboard

A tela inicial deverá apresentar uma visão resumida do negócio.

## Indicadores principais

```text
Faturamento
R$ 8.450,00

Custo dos produtos
R$ 4.200,00

Lucro bruto
R$ 4.250,00

Despesas
R$ 1.350,00

Lucro líquido
R$ 2.900,00
```

## Outras informações

- Produtos mais vendidos
- Produtos mais lucrativos
- Produtos com baixa margem
- Estoque baixo
- Evolução do faturamento
- Evolução do lucro
- Ticket médio
- Margem média
- Ponto de equilíbrio

---

# 13. Roadmap do MVP

A implementação deve ser incremental.

## Fase 1 — Cadastros

- [ ] Produtos
- [ ] Categorias
- [ ] Fornecedores
- [ ] Clientes

## Fase 2 — Estoque

- [ ] Entrada de produtos
- [ ] Saída de produtos
- [ ] Ajustes de estoque
- [ ] Controle de estoque mínimo
- [ ] Histórico de movimentações
- [ ] Cálculo de custo

## Fase 3 — Vendas

- [ ] Cadastro de venda
- [ ] Itens da venda
- [ ] Desconto
- [ ] Forma de pagamento
- [ ] Cliente
- [ ] Baixa automática do estoque
- [ ] Cálculo do lucro da venda

## Fase 4 — Financeiro

- [ ] Receitas
- [ ] Despesas
- [ ] Custos
- [ ] Taxas
- [ ] Resultado financeiro
- [ ] Lucro líquido

## Fase 5 — Dashboard

- [ ] Faturamento
- [ ] Lucro bruto
- [ ] Lucro líquido
- [ ] Margem
- [ ] Ticket médio
- [ ] Produtos mais vendidos
- [ ] Produtos mais lucrativos
- [ ] Estoque baixo
- [ ] Ponto de equilíbrio

---

# 14. Arquitetura inicial

Como projeto pessoal, a arquitetura pode seguir uma estrutura semelhante à utilizada em sistemas profissionais.

```text
Frontend
    ↓
API REST
    ↓
Backend
    ↓
PostgreSQL
```

Possível evolução:

```text
                    ┌── Produtos
                    ├── Estoque
                    ├── Compras
Frontend → API → ───┼── Vendas
                    ├── Financeiro
                    ├── Clientes
                    └── Dashboard
                         ↓
                    PostgreSQL
```

A arquitetura deverá priorizar inicialmente simplicidade e facilidade de evolução.

---

# 15. Possível stack

Como o objetivo também pode ser utilizar o projeto para aprendizado e portfólio:

### Backend

- Java
- Spring Boot
- API REST
- Spring Data JPA
- Bean Validation

### Banco

- PostgreSQL

### Frontend

A tecnologia poderá ser definida posteriormente.

Possibilidades:

- React
- Angular
- Vue
- Outra tecnologia adequada ao objetivo do projeto

### Infraestrutura futura

- Docker
- CI/CD
- Cloud
- Monitoramento
- Backup

Esses itens não fazem parte do MVP inicial.

---

# 16. Possível evolução para SaaS

Inicialmente:

```text
Sistema para controle das vendas de perfumes
da mãe
```

Posteriormente:

```text
Sistema de gestão para pequenos negócios
```

O sistema poderia atender:

- Perfumarias
- Vendedores de cosméticos
- Roupas
- Acessórios
- Doces
- Artesanato
- Pequenos revendedores
- Vendedores autônomos

A principal proposta de valor seria:

> **Ajudar pequenos empreendedores a entender quanto realmente estão ganhando com suas vendas.**

---

# 17. Possíveis nomes

Algumas opções levantadas:

- Lucra
- Margem
- VendeBem
- ContaCerta
- Meu Negócio
- VendaFácil
- VendeMais
- Gestor+
- Negócio+

### Preferências iniciais

**Lucra**

> Veja quanto seu negócio realmente lucra.

**Margem**

> Entenda sua margem. Controle seu negócio.

**VendeBem**

> Venda, controle e cresça.

Para uma futura transformação em SaaS, nomes mais genéricos como **Lucra**, **Margem** ou **VendeBem** permitem expandir além do segmento de perfumes.

---

# 18. Princípios do projeto

1. Começar simples.
2. Resolver primeiro uma necessidade real.
3. Evitar funcionalidades desnecessárias no MVP.
4. Manter histórico das movimentações financeiras e de estoque.
5. Não sobrescrever informações históricas importantes.
6. Separar custo, receita e despesa.
7. Calcular lucro com base no custo correto da mercadoria.
8. Transformar dados em indicadores fáceis de entender.
9. Projetar o domínio de forma que o sistema possa evoluir para outros tipos de negócio.
10. Priorizar usabilidade para pessoas que não possuem conhecimento técnico ou contábil.

---

# 19. Próximo passo

Antes de começar a implementação, definir o **modelo de domínio do sistema**, principalmente:

- Entidades
- Relacionamentos
- Regras de estoque
- Regras de custo
- Regras de venda
- Cálculo de margem
- Cálculo de lucro
- Tratamento de despesas
- Modelo financeiro

A partir disso, definir o modelo do banco de dados e somente depois iniciar a implementação da API.
