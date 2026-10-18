# workshop-service

API de gestão de oficina mecânica (clientes, veículos, ordens de serviço e estoque).

## Pré-requisitos

- **JDK 25 (Eclipse Temurin)**. É a mesma distribuição usada no Docker e no CI.
  - Windows: `winget install EclipseAdoptium.Temurin.25.JDK`
  - Linux/macOS: `sdk install java 25-tem` (SDKMAN) ou o instalador em <https://adoptium.net>

Não é preciso instalar o Gradle: use o wrapper (`./gradlew`, ou `gradlew.bat` no Windows).

## Como rodar

```bash
# Compila e roda todos os testes
./gradlew build

# Sobe a aplicação em http://localhost:8080
./gradlew bootRun

# Verifica se está no ar
curl http://localhost:8080/actuator/health
# {"groups":["liveness","readiness"],"status":"UP"}
```

## Contribuindo

Leia o [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir o primeiro PR.
