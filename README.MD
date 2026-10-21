# workshop-service

API de gestão de oficina mecânica (clientes, veículos, ordens de serviço e estoque).

## Pré-requisitos

- **JDK 25 (Eclipse Temurin)**. É a mesma distribuição usada no Docker e no CI.
  - Windows: `winget install EclipseAdoptium.Temurin.25.JDK`
  - Linux/macOS: `sdk install java 25-tem` (SDKMAN) ou o instalador em <https://adoptium.net>
- **Docker** rodando. Os testes de integração e o `bootTestRun` sobem um PostgreSQL descartável via Testcontainers.

Não é preciso instalar o Gradle: use o wrapper (`./gradlew`, ou `gradlew.bat` no Windows).

## Como rodar

```bash
# Compila e roda todos os testes
./gradlew build

# Sobe a aplicação em http://localhost:8080 com um PostgreSQL descartável (os dados somem ao parar)
./gradlew bootTestRun

# Verifica se está no ar
curl http://localhost:8080/actuator/health
# {"groups":["liveness","readiness"],"status":"UP"}
```

Para usar um PostgreSQL próprio, rode `./gradlew bootRun`. Por padrão ele conecta em `jdbc:postgresql://localhost:5432/workshop` com usuário e senha `workshop`; para mudar, defina `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. As migrations do Flyway são aplicadas automaticamente na subida.

## Decisões de arquitetura

- [ADR 001: Banco de dados e persistência](docs/adr/001-database.md)

## Contribuindo

Leia o [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir o primeiro PR.
