# workshop-service

[![CI](https://github.com/ArkTech-LVW/workshop-service/actions/workflows/ci.yml/badge.svg)](https://github.com/ArkTech-LVW/workshop-service/actions/workflows/ci.yml)

API de gestão de oficina mecânica (clientes, veículos, ordens de serviço e estoque).

## Pré-requisitos

- **JDK 25 (Eclipse Temurin)**. É a mesma distribuição usada no Docker e no CI.
  - Windows: `winget install EclipseAdoptium.Temurin.25.JDK`
  - Linux/macOS: `sdk install java 25-tem` (SDKMAN) ou o instalador em <https://adoptium.net>
- **Docker** rodando. O `bootRun` sobe o PostgreSQL do `docker-compose.yml`, e os testes de integração sobem um PostgreSQL descartável via Testcontainers.

Não é preciso instalar o Gradle: use o wrapper (`./gradlew`, ou `gradlew.bat` no Windows).

## Como rodar

### Com Docker (ambiente completo)

Só precisa do Docker. Sobe o PostgreSQL e a aplicação em http://localhost:8080:

```bash
docker compose up --build
```

Usuário e senha do banco vêm das variáveis `DB_USERNAME` e `DB_PASSWORD`, lidas de um arquivo `.env` na raiz (fora do git). Sem `.env`, os dois valem `workshop`.

### Para desenvolvimento

```bash
# Compila, roda todos os testes e gera o relatório de cobertura (build/reports/jacoco/test/html)
./gradlew build

# Roda só os testes de integração (aceita --tests para filtrar uma classe)
./gradlew integrationTest

# Sobe só o PostgreSQL do docker-compose.yml e roda a aplicação localmente em http://localhost:8080
./gradlew bootRun

# Verifica se está no ar
curl http://localhost:8080/actuator/health
# {"groups":["liveness","readiness"],"status":"UP"}
```

As migrations do Flyway são aplicadas automaticamente na subida. Quando a aplicação para, o container do PostgreSQL também para, mas os dados continuam no volume `postgres-data` (`docker compose down -v` apaga tudo).

Para usar um PostgreSQL próprio, desligue o compose com `SPRING_DOCKER_COMPOSE_ENABLED=false` e defina `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` (padrão: `jdbc:postgresql://localhost:5432/workshop`, usuário e senha `workshop`).

### Perfis: desenvolvimento e produção

| Como roda | Perfil | O que precisa |
|---|---|---|
| IntelliJ, `./gradlew bootRun` ou o jar na sua máquina | `development` (padrão do `application.yml`) | nada |
| `docker compose up` | `development` (definido no `docker-compose.yml`) | nada |
| Imagem Docker num servidor | `production` (definido no `Dockerfile`) | `JWT_SECRET` e as variáveis do banco |
| Jar direto num servidor | `production` | `SPRING_PROFILES_ACTIVE=production`, `JWT_SECRET` e as variáveis do banco |

O perfil `development` assina os tokens JWT com uma chave de desenvolvimento, que é pública no repositório. Fora dele, a variável `JWT_SECRET` é obrigatória, com pelo menos 32 bytes (para gerar uma: `openssl rand -base64 48`); sem ela, a aplicação não sobe. Nunca rode num servidor sem `SPRING_PROFILES_ACTIVE=production`: em `development`, qualquer um consegue gerar um token válido.

### Usuário ADMIN local

No perfil `development`, a aplicação cria na subida, se ainda não existir, um usuário ADMIN só para ambiente local: **`admin@workshop.local`**, senha **`admin123`**. No perfil `production`, nenhum usuário é criado.

## Autenticação

```bash
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email": "admin@workshop.local", "password": "admin123"}'
# {"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":3600}
```

Envie o `accessToken` no header `Authorization: Bearer <token>` das rotas protegidas. O token vale 1h.

O login e as demais rotas têm limite de requisições (`security.rate-limit` no `application.yml`); acima dele, a API responde 429. O limite usa o IP do cliente: atrás de um proxy reverso, o `X-Forwarded-For` só é aceito de endereços privados. Se o proxy usar outro endereço, configure `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` com ele.

## Documentação da API

Com a aplicação no ar, a documentação (Swagger UI) fica em <http://localhost:8080/docs>, com todas as rotas, os contratos e o formato dos erros. A especificação OpenAPI em JSON está em <http://localhost:8080/docs/openapi.json>.

Para chamar rotas protegidas pela própria UI, faça o login em `POST /auth/login`, clique em **Authorize** e cole o `accessToken`.

## Métricas de tempo da OS

As métricas saem do histórico de status de cada OS. A consulta e o índice estão no [ADR 008](docs/adr/008-metrics-queries.md).

- **Quais OS entram:** as que entraram em Finalizada (`COMPLETED`) dentro do período, contado em dias inteiros no fuso da oficina (`America/Sao_Paulo`), com o início e o fim inclusos. Ficam de fora as canceladas e as que pararam em Orçamento rejeitado. Uma OS rejeitada, reaberta e depois executada entra normalmente.
- **Tempo em um status:** soma de todos os períodos que a OS passou nele. Cada entrada no status conta até a mudança de status seguinte. O status atual da OS não conta, porque o período dele ainda não terminou: o tempo em Finalizada só aparece para as OS já entregues.
- **Tempo de execução:** tempo em Em execução (`IN_PROGRESS`). Com o orçamento complementar, a OS entra em execução mais de uma vez, e os períodos se somam. A espera pela aprovação do cliente e a fila até o mecânico iniciar (Orçamento aprovado e Complementar aprovado) ficam de fora.
- **Média:** por status, entre as OS que passaram por ele, junto com a quantidade dessas OS. Sem OS no período, a média é nula.
- **Por serviço do catálogo:** o tempo de cada serviço vai do início à conclusão dele na OS (`POST /admin/serviceOrders/{id}/services/{itemId}/start` e `/complete`), contando só os períodos em Em execução. A média é por unidade, dividida pela soma das quantidades, e fica comparável com o tempo estimado. Junto vêm a quantidade de OS que incluíram o serviço e a de execuções. Nome e tempo estimado são os atuais do catálogo.
- **Rota:** `GET /admin/metrics/averageExecutionTime?from=2026-10-01&to=2026-10-31`, só para ADMIN, com no máximo 366 dias. Cada duração sai em minutos inteiros e em `HH:mm`, com as horas sem limite (`27:05`).

## Decisões de arquitetura

- [ADR 001: Banco de dados e persistência](docs/adr/001-database.md)
- [ADR 002: Padrão de erros da API](docs/adr/002-api-errors.md)
- [ADR 003: Autenticação e autorização](docs/adr/003-authentication.md)
- [ADR 004: Eventos de domínio](docs/adr/004-domain-events.md)
- [ADR 005: Acesso do cliente à ordem de serviço](docs/adr/005-customer-access.md)
- [ADR 006: Orçamento complementar durante a execução](docs/adr/006-supplementary-quote.md)
- [ADR 007: Versionamento de migrations por timestamp e execução fora de ordem](docs/adr/007-timestamp-migrations.md)
- [ADR 008: Consultas de métricas sobre tabelas de outros módulos](docs/adr/008-metrics-queries.md)
- [ADR 009: Reserva de estoque no orçamento](docs/adr/009-stock-reservation.md)

## Contribuindo

Leia o [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir o primeiro PR.
