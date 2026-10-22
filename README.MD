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

```bash
# Compila, roda todos os testes e gera o relatório de cobertura (build/reports/jacoco/test/html)
./gradlew build

# Sobe o PostgreSQL (docker-compose.yml) e a aplicação em http://localhost:8080
./gradlew bootRun

# Verifica se está no ar
curl http://localhost:8080/actuator/health
# {"groups":["liveness","readiness"],"status":"UP"}
```

As migrations do Flyway são aplicadas automaticamente na subida. Quando a aplicação para, o container do PostgreSQL também para, mas os dados continuam no volume `postgres-data` (`docker compose down -v` apaga tudo).

Para usar um PostgreSQL próprio, desligue o compose com `SPRING_DOCKER_COMPOSE_ENABLED=false` e defina `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` (padrão: `jdbc:postgresql://localhost:5432/workshop`, usuário e senha `workshop`).

## Decisões de arquitetura

- [ADR 001: Banco de dados e persistência](docs/adr/001-database.md)
- [ADR 002: Padrão de erros da API](docs/adr/002-api-errors.md)

## Contribuindo

Leia o [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir o primeiro PR.
