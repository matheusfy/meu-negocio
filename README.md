# Sistema de Gestão de Pedidos

Projeto de estudos que simula, do zero, um sistema de gestão de pedidos em Java — cobrindo desde o modelo de domínio até deploy em nuvem com observabilidade e infraestrutura como código. O objetivo é praticar arquitetura de software, boas práticas de backend e o ciclo completo de DevOps em um único projeto guiado por fases incrementais.

## O que o sistema faz

Gerencia o ciclo de vida de pedidos de um cliente: criação, consulta, listagem por cliente, cancelamento e mudança de status. Ao longo das fases, o sistema evolui para incluir autenticação e autorização, reserva de estoque orientada a eventos, cache de consultas, observabilidade (métricas, dashboards, alertas) e provisionamento automatizado em nuvem.

## Tecnologias utilizadas

**Linguagem e core**
- Java 21 (com uso de virtual threads em pontos que se beneficiam de paralelismo)
- Spring Boot (Web, Security, Data JPA, Validation)
- Maven

**Persistência e cache**
- PostgreSQL (dados transacionais)
- H2 (banco em memória para testes rápidos)
- Redis (cache de consultas e/ou sessão)

**Mensageria**
- RabbitMQ ou Kafka (eventos como `PedidoCriado` e `PedidoRejeitado`)

**Segurança**
- Spring Security, hash de senha com BCrypt, JWT (ou sessão stateful via Redis)

**Testes**
- JUnit 5, Mockito, Testcontainers, RestAssured/MockMvc, Jacoco (cobertura), Cucumber (opcional, BDD)

**Observabilidade**
- Spring Boot Actuator, Micrometer, Prometheus, Grafana

**Infraestrutura e deploy**
- Docker (build multi-stage) e Docker Compose
- GitHub Actions (CI/CD)
- Terraform (VPC, RDS, ElastiCache, ECS Fargate/EC2 + ALB, Amazon MQ/MSK)
- AWS

## Arquitetura

O projeto segue uma organização em camadas (domínio, aplicação, infraestrutura e API REST), com entidades e agregados bem definidos (ex: `Pedido` como aggregate root, `ItemPedido`, `Endereco` como value object, `StatusPedido` como enum). Regras de negócio são implementadas com uso de Streams e DTOs via `record`, com validação de entrada e tratamento de exceções centralizado.

## Como executar o projeto

### Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker e Docker Compose

### Subindo as dependências (Postgres, Redis, RabbitMQ)

```bash
docker-compose up -d
```

### Executando a aplicação

```bash
./mvnw spring-boot:run
```

### Rodando os testes

```bash
./mvnw test
```

### Build e execução via Docker

```bash
docker build -t sistema-gestao-pedidos .
docker run -p 8080:8080 sistema-gestao-pedidos
```

## Roadmap do projeto

O desenvolvimento é guiado por fases incrementais, cada uma rastreada como issues neste repositório:

| Fase | Foco |
|------|------|
| 1 | Modelagem de domínio (entidades, value objects, agregados) |
| 2 | API REST, JPA, regras de negócio, DTOs, validação e testes unitários |
| 3 | Autenticação, autorização, cache com Redis e multi-datasource |
| 4 | Mensageria assíncrona (eventos de pedido, estoque e notificação) e concorrência |
| 5 | Testes de integração, funcionais e cobertura de código |
| 6 | Observabilidade: métricas, Prometheus e dashboards Grafana |
| 7 | Containerização com Docker e Docker Compose |
| 8 | Pipeline de CI/CD (GitHub Actions) |
| 9 | Infraestrutura como código com Terraform na AWS |
| 10 | Deploy em nuvem, validação de observabilidade e runbook operacional |

## Status

Projeto em desenvolvimento ativo, seguindo o roadmap acima fase a fase.
