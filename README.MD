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

# Sobe só o PostgreSQL do docker-compose.yml e roda a aplicação localmente em http://localhost:8080
./gradlew bootRun

# Verifica se está no ar
curl http://localhost:8080/actuator/health
# {"groups":["liveness","readiness"],"status":"UP"}
```

As migrations do Flyway são aplicadas automaticamente na subida. Quando a aplicação para, o container do PostgreSQL também para, mas os dados continuam no volume `postgres-data` (`docker compose down -v` apaga tudo).

Para usar um PostgreSQL próprio, desligue o compose com `SPRING_DOCKER_COMPOSE_ENABLED=false` e defina `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` (padrão: `jdbc:postgresql://localhost:5432/workshop`, usuário e senha `workshop`).

### Chave dos tokens (`JWT_SECRET`)

Localmente, os tokens JWT são assinados com uma chave de desenvolvimento que já vem no `application.yml`, sem precisar configurar nada.

Em qualquer outro ambiente, defina a variável `JWT_SECRET`, com pelo menos 32 bytes (para gerar uma: `openssl rand -base64 48`). A chave de desenvolvimento é pública no repositório: com ela, qualquer um consegue gerar um token válido.

### Perfil `development` e usuário ADMIN local

O `bootRun` e o `docker compose up` ativam o perfil `development`, que cria na subida, se ainda não existir, um usuário ADMIN só para ambiente local: **`admin@workshop.local`**, senha **`admin123`**.

Rodando a aplicação de outro jeito (o jar ou a classe `WorkshopApplication` na IDE sem perfil), nenhum usuário é criado. Na IDE, ative o perfil `development` na configuração de execução (*Active profiles*).

## Autenticação

```bash
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email": "admin@workshop.local", "password": "admin123"}'
# {"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":3600}
```

Envie o `accessToken` no header `Authorization: Bearer <token>` das rotas protegidas. O token vale 1h.

## Documentação da API

Com a aplicação no ar, a documentação (Swagger UI) fica em <http://localhost:8080/docs>, com todas as rotas, os contratos e o formato dos erros. A especificação OpenAPI em JSON está em <http://localhost:8080/docs/openapi.json>.

Para chamar rotas protegidas pela própria UI, faça o login em `POST /auth/login`, clique em **Authorize** e cole o `accessToken`.

## Decisões de arquitetura

- [ADR 001: Banco de dados e persistência](docs/adr/001-database.md)
- [ADR 002: Padrão de erros da API](docs/adr/002-api-errors.md)
- [ADR 003: Autenticação e autorização](docs/adr/003-authentication.md)

## Contribuindo

Leia o [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir o primeiro PR.
