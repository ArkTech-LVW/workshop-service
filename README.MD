# workshop-service

[![CI](https://github.com/ArkTech-LVW/workshop-service/actions/workflows/ci.yml/badge.svg)](https://github.com/ArkTech-LVW/workshop-service/actions/workflows/ci.yml)

API de gestão de oficina mecânica: clientes, veículos, catálogo de serviços, estoque de peças e ordens de serviço, do recebimento do veículo à entrega.

## Sumário

- [Objetivo](#objetivo)
- [A solução](#a-solução)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Pré-requisitos](#pré-requisitos)
- [Como executar](#como-executar)
- [Usuários de demonstração e papéis](#usuários-de-demonstração-e-papéis)
- [Autenticação](#autenticação)
- [Documentação da API](#documentação-da-api)
- [Notificações por e-mail](#notificações-por-e-mail)
- [Métricas de tempo da OS](#métricas-de-tempo-da-os)
- [Testes](#testes)
- [Análise de vulnerabilidades](#análise-de-vulnerabilidades)
- [Documentação do projeto](#documentação-do-projeto)
- [Contribuindo](#contribuindo)

## Objetivo

Dar a uma oficina mecânica um back-end único para acompanhar cada veículo do recebimento à entrega: diagnóstico, orçamento, aprovação pelo cliente, execução e entrega, com o estoque de peças atualizado e o tempo de cada etapa medido. O cliente acompanha a própria OS e aprova o orçamento sem precisar de conta.

## A solução

| Módulo | O que faz |
|---|---|
| `auth` | Login com JWT, usuários internos e papéis (ADMIN, ATTENDANT, MECHANIC) |
| `registry` | Clientes (CPF ou CNPJ validados) e veículos (placa antiga ou Mercosul) |
| `catalog` | Catálogo de serviços, com preço e tempo estimado |
| `inventory` | Peças e insumos, movimentações de estoque, reserva para orçamentos e pedidos de ajuste aprovados pelo ADMIN |
| `serviceorder` | Ordem de serviço: itens, orçamento, aprovação, orçamento complementar, execução por serviço, entrega e histórico de status |
| `notification` | E-mail ao cliente quando o orçamento é enviado, aprovado ou rejeitado e quando a OS é finalizada |
| `metrics` | Tempo médio de execução e de cada status, geral e por serviço do catálogo |
| `shared` | O que todos os módulos usam: erros, i18n, segurança, contratos entre módulos e eventos de domínio |

O ciclo de vida da OS:

```
RECEIVED ─> IN_DIAGNOSIS ─> AWAITING_APPROVAL ─> QUOTE_APPROVED ─> IN_PROGRESS ─> COMPLETED ─> DELIVERED
                 ▲                 │                                 │     ▲
                 │                 ▼                                 ▼     │
                 └──────── QUOTE_REJECTED       AWAITING_SUPPLEMENTARY_APPROVAL ─> SUPPLEMENTARY_APPROVED
                          (reabre o orçamento)                       │
                                                                     ▼
                                                       SUPPLEMENTARY_REJECTED (novo complementar)
```

Fora desse caminho, a OS pode ir para `CANCELED` (ver cancelamento abaixo).

- **Orçamento:** soma os serviços do catálogo e as peças. Ao ser enviado, reserva as peças por 7 dias ([ADR 009](docs/adr/009-stock-reservation.md)); na aprovação, o estoque é baixado.
- **Aprovação pelo cliente:** pela rota pública da OS, com o `publicId` e o CPF/CNPJ dele, sem login ([ADR 005](docs/adr/005-customer-access.md)). A oficina também pode aprovar no balcão.
- **Orçamento complementar:** um item novo durante a execução gera um orçamento complementar. O cliente aprova tudo ou só o original ([ADR 006](docs/adr/006-supplementary-quote.md)).
- **Execução:** cada serviço da OS é iniciado e concluído pelo mecânico. Quando o último termina, a OS vai sozinha para `COMPLETED`.
- **Cancelamento:** permitido enquanto o orçamento inicial não foi aprovado e, depois disso, só enquanto o complementar está pendente ou rejeitado.
- **Histórico:** toda mudança de status é registrada com data, usuário e observação, e é a base das [métricas](#métricas-de-tempo-da-os).

## Arquitetura

Monólito modular orientado a DDD. Cada módulo é um contexto delimitado com o seu próprio modelo de domínio: agregados, entidades, value objects, repositórios e eventos de domínio. Os contextos não importam classes uns dos outros: referenciam-se por id, conversam por contratos no `shared` (implementados pelo contexto dono) e reagem a eventos de integração ([ADR 004](docs/adr/004-domain-events.md)). O `shared/domain` é o núcleo compartilhado, com os value objects comuns a todos os contextos. Dentro de cada módulo, as camadas seguem a regra de dependência abaixo:

```
api ──> application ──> domain <── infrastructure
```

| Camada | Conteúdo |
|---|---|
| `api` | Controllers REST, DTOs de entrada e saída e a documentação OpenAPI |
| `application` | Casos de uso (um por pacote, com command/query e result), transações e listeners de eventos |
| `domain` | Entidades, value objects, regras de negócio, interfaces de repositório e ports |
| `infrastructure` | Implementações técnicas: repositórios Spring Data, SMTP, JWT, rate limit, configurações |

O isolamento entre módulos e a regra entre camadas são verificados pelo teste ArchUnit `LayerDependencyTest`: uma violação quebra o build. As convenções completas estão no [CONTRIBUTING.md](CONTRIBUTING.md#estrutura-de-pacotes).

### Elementos de DDD

**Estratégicos**

| Elemento | No projeto |
|---|---|
| Contextos delimitados | Um por módulo: `auth`, `registry`, `catalog`, `inventory`, `serviceorder`, `notification` e `metrics` |
| Núcleo compartilhado | `shared/domain`: `Money`, `CPF`, `CNPJ`, `CPFCNPJ`, `Email`, `Phone`, `LicensePlate`, `Chassis`, `Role`, `BaseEntity` e as exceções de domínio |
| Contratos entre contextos | `CustomerDirectory`, `CustomerContacts`, `VehicleDirectory`, `PartDirectory`, `ServiceOfferingDirectory`, `Notifier` e `CurrentUser`, no `shared/application`. O contexto consumidor recebe DTOs do contrato (`PartDetails`, `VehicleDetails`), nunca a entidade do outro |
| Referência entre contextos | Só por id: a OS guarda `customerId`, `vehicleId` e `mechanicId`; o veículo guarda `customerId` |
| Eventos de integração | `QuoteSentEvent`, `QuoteApprovedEvent`, `QuoteRejectedEvent`, `QuoteWithdrawnEvent`, `ServiceOrderCanceledEvent` e `ServiceOrderCompletedEvent`, em `shared/event/serviceorder`. O `inventory` reserva, baixa e libera o estoque, e as notificações saem para o cliente |
| Linguagem ubíqua | Nos nomes do domínio: `sendQuote`, `approveOriginalOnly`, `reopenQuote`, `startDiagnosis`, `StockReservation`, `ServiceOrderNumber` |

**Táticos**

| Elemento | No projeto |
|---|---|
| Agregados | `ServiceOrder` (raiz de `ServiceItem`, `PartItem` e `StatusChange`, que só mudam por ela e não têm repositório próprio), `User` (raiz de `UserRole`), `Customer`, `Vehicle`, `ServiceOffering`, `Part`, `StockMovement`, `StockReservation`, `StockAdjustmentRequest` e `EmailNotification` |
| Entidades | Todas estendem a `BaseEntity`, com identidade e datas de criação e alteração |
| Value objects | Os do núcleo compartilhado, mais `Sku` e `ServiceOrderNumber`: imutáveis e validados na criação |
| Modelo rico | As regras ficam nos agregados: a máquina de estados da OS (`ServiceOrderStatus` e `InvalidStatusTransitionException`), `Part.reserve` e `Part.requireAvailable`, `StockAdjustmentRequest.approve` |
| Eventos de domínio | Registrados pelo agregado `ServiceOrder` e publicados no `save` com `@DomainEvents` |
| Repositórios | Interfaces no `domain/repository`, implementadas pelo Spring Data na `infrastructure` |
| Fábricas | `StockReservation.reserve` e `StockAdjustmentRequest.approve`, que gera o `StockMovement` |
| Serviços de aplicação | Os casos de uso (`UseCase` e `VoidUseCase`), um por pacote, com command ou query e result |
| Ports | `PasswordHasher` e `EmailSender` no domínio; `TokenIssuer` e `ServiceOrderHistoryReader` na aplicação |
| Exceções de domínio | `DomainException` e as filhas `BusinessRuleException`, `NotFoundException` e `ConflictException` |

Não há serviços de domínio: toda regra coube em um agregado.

**Decisões pragmáticas**

- As entidades de domínio são as próprias entidades JPA, sem um modelo de persistência separado ([ADR 001](docs/adr/001-database.md)).
- O agregado declara `@DomainEvents` e `@AfterDomainEventPublication`, as únicas anotações do Spring no domínio, para os eventos saírem sozinhos no `save` ([ADR 004](docs/adr/004-domain-events.md)).
- O `metrics` é um contexto só de leitura, sem camada de domínio: consulta o histórico da OS direto no banco ([ADR 008](docs/adr/008-metrics-queries.md)).

### Estrutura de diretórios

```
workshop-service
├── .github/workflows             CI, evidência de cobertura e bloqueio de PR
├── docs
│   ├── adr                       decisões de arquitetura
│   ├── evidencias/cobertura      evidência versionada da cobertura de testes
│   ├── postman                   como gerar e usar a collection do Postman
│   └── security                  relatório de vulnerabilidades (SonarQube)
├── gradle                        wrapper e catálogo de versões (libs.versions.toml)
├── src/main/java/br/com/arktech/workshop
│   ├── auth | catalog | inventory | metrics | notification | registry | serviceorder
│   │   ├── api
│   │   ├── application
│   │   ├── domain
│   │   └── infrastructure
│   └── shared
├── src/main/resources
│   ├── db/migration              migrations do Flyway, versionadas por timestamp
│   ├── openapi                   textos da documentação da API
│   ├── templates/email           templates Thymeleaf dos e-mails
│   ├── messages_pt_BR.properties textos das mensagens e erros da API
│   └── notifications_pt_BR.properties textos dos e-mails
├── src/test/java                 testes unitários, de API e de integração, espelhando os pacotes de main
├── Dockerfile                    imagem da aplicação, em duas etapas
└── docker-compose.yml            PostgreSQL, Mailpit e a aplicação
```

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 25 (Eclipse Temurin) | Linguagem e runtime |
| Spring Boot 4.1 | Web MVC, Validation, Data JPA, Security (OAuth2 Resource Server com JWT), Mail, Actuator |
| PostgreSQL 18 | Banco de dados ([ADR 001](docs/adr/001-database.md)) |
| Flyway | Migrations, versionadas por timestamp ([ADR 007](docs/adr/007-timestamp-migrations.md)) |
| springdoc-openapi 3 | OpenAPI e Swagger UI em `/docs` |
| Thymeleaf | Templates HTML dos e-mails |
| Bucket4j e Caffeine | Rate limit por usuário, IP e e-mail do login |
| Lombok | Redução de código repetitivo |
| Error Prone e NullAway | Checagem de nulabilidade na compilação, com JSpecify |
| JUnit 5, Mockito, Testcontainers e ArchUnit | Testes unitários, de integração e de arquitetura |
| JaCoCo | Cobertura de testes, com mínimo verificado no build |
| SonarQube | Análise estática de código e de vulnerabilidades |
| Gradle (Kotlin DSL) | Build, com wrapper |
| Docker e Docker Compose | Ambiente local completo e imagem da aplicação |
| Mailpit | Servidor SMTP de teste para o ambiente local |
| GitHub Actions | CI em todo PR e push na `main` |

## Pré-requisitos

- **Docker** rodando. É o único requisito para subir tudo com `docker compose`. No desenvolvimento, o `bootRun` sobe o PostgreSQL e o Mailpit do `docker-compose.yml`, e os testes de integração sobem um PostgreSQL descartável via Testcontainers.
- **JDK 25 (Eclipse Temurin)**, só para desenvolver ou rodar os testes. É a mesma distribuição usada no Docker e no CI.
  - Windows: `winget install EclipseAdoptium.Temurin.25.JDK`
  - Linux/macOS: `sdk install java 25-tem` (SDKMAN) ou o instalador em <https://adoptium.net>

Não é preciso instalar o Gradle: use o wrapper (`./gradlew`, ou `gradlew.bat` no Windows).

## Como executar

### Com Docker (ambiente completo)

Só precisa do Docker. Sobe o PostgreSQL, o Mailpit (servidor de e-mail local) e a aplicação em http://localhost:8080:

```bash
docker compose up --build
```

Quando o container `app` ficar `healthy`, a aplicação está pronta:

```bash
curl http://localhost:8080/actuator/health
# {"groups":["liveness","readiness"],"status":"UP"}
```

Pronto para usar:

| O quê | Onde |
|---|---|
| API | <http://localhost:8080> |
| Documentação (Swagger UI) | <http://localhost:8080/docs> |
| Caixa de entrada do Mailpit | <http://localhost:8025> |
| Login | `admin@workshop.local` / `admin123` ([detalhes](#usuários-de-demonstração-e-papéis)) |

Usuário e senha do banco vêm das variáveis `DB_USERNAME` e `DB_PASSWORD`, lidas de um arquivo `.env` na raiz (fora do git). Sem `.env`, os dois valem `workshop`. Para parar, `docker compose down`; para apagar também os dados, `docker compose down -v`.

### Para desenvolvimento

```bash
# Sobe o PostgreSQL e o Mailpit do docker-compose.yml e roda a aplicação localmente em http://localhost:8080
./gradlew bootRun
```

As migrations do Flyway são aplicadas automaticamente na subida. Quando a aplicação para, os containers do PostgreSQL e do Mailpit também param, mas os dados continuam no volume `postgres-data`.

Para usar um PostgreSQL próprio, desligue o compose com `SPRING_DOCKER_COMPOSE_ENABLED=false` e defina `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` (padrão: `jdbc:postgresql://localhost:5432/workshop`, usuário e senha `workshop`).

### Perfis: desenvolvimento e produção

| Como roda | Perfil | O que precisa |
|---|---|---|
| IntelliJ, `./gradlew bootRun` ou o jar na sua máquina | `development` (padrão do `application.yml`) | nada |
| `docker compose up` | `development` (definido no `docker-compose.yml`) | nada |
| Imagem Docker num servidor | `production` (definido no `Dockerfile`) | `JWT_SECRET` e as variáveis do banco |
| Jar direto num servidor | `production` | `SPRING_PROFILES_ACTIVE=production`, `JWT_SECRET` e as variáveis do banco |

O perfil `development` assina os tokens JWT com uma chave de desenvolvimento, que é pública no repositório. Fora dele, a variável `JWT_SECRET` é obrigatória, com pelo menos 32 bytes (para gerar uma: `openssl rand -base64 48`); sem ela, a aplicação não sobe. Nunca rode num servidor sem `SPRING_PROFILES_ACTIVE=production`: em `development`, qualquer um consegue gerar um token válido.

### Variáveis de ambiente

| Variável | Padrão | Para quê |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `development` | Perfil (ver acima) |
| `DB_URL` | `jdbc:postgresql://localhost:5432/workshop` | Banco de dados |
| `DB_USERNAME` / `DB_PASSWORD` | `workshop` / `workshop` | Banco de dados |
| `JWT_SECRET` | só no `development` | Chave de assinatura dos tokens, com pelo menos 32 bytes |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` (Mailpit) | Servidor SMTP |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | vazios | Autenticação no SMTP |
| `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` | `false` / `false` | Autenticação e TLS no SMTP |
| `MAIL_FROM` | `oficina@arktech.local` | Remetente dos e-mails |
| `SPRING_DOCKER_COMPOSE_ENABLED` | `true` | `false` para o `bootRun` não subir o compose |
| `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` | endereços privados | Proxies aceitos no `X-Forwarded-For` ([rate limit](#autenticação)) |

## Usuários de demonstração e papéis

No perfil `development`, a aplicação cria na subida, se ainda não existir, um usuário ADMIN só para ambiente local:

| E-mail | Senha | Papel |
|---|---|---|
| `admin@workshop.local` | `admin123` | ADMIN |

No perfil `production`, nenhum usuário é criado. Para demonstrar os outros papéis, faça o login como ADMIN e crie os usuários em `POST /admin/users`:

```json
{ "name": "Ana Souza", "email": "ana@workshop.com", "password": "senha1234", "roles": ["ATTENDANT"] }
```

Um usuário pode ter mais de um papel. O que cada papel pode fazer:

| Ação | ADMIN | ATTENDANT | MECHANIC |
|---|:-:|:-:|:-:|
| Usuários, clientes, catálogo de serviços, peças e movimentações de estoque | ✓ | | |
| Aprovar ou rejeitar pedidos de ajuste de estoque | ✓ | | |
| Pedir ajuste de estoque | ✓ | ✓ | |
| Veículos (cadastrar, consultar, alterar) | ✓ | ✓ | |
| Excluir veículo | ✓ | | |
| Abrir, listar e consultar OS | ✓ | ✓ | |
| Aprovar ou rejeitar orçamento no balcão, entregar e cancelar OS | ✓ | ✓ | |
| Incluir e remover itens da OS e enviar o orçamento | ✓ | ✓ | ✓ |
| Iniciar diagnóstico e execução, iniciar e concluir serviços, finalizar OS | ✓ | | ✓ |
| Reabrir orçamento rejeitado ou retirar orçamento enviado | ✓ | | |
| Métricas | ✓ | | |

Um papel fora da lista recebe 403. No Swagger, cada rota protegida diz no fim da descrição quais papéis podem chamá-la. O cliente da oficina não tem usuário: ele acessa a própria OS pelas rotas `/customer/...`, como explica a [autenticação](#autenticação).

## Autenticação

```bash
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email": "admin@workshop.local", "password": "admin123"}'
# {"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":3600}
```

Envie o `accessToken` no header `Authorization: Bearer <token>` das rotas protegidas. O token vale 1h; depois disso, as rotas respondem 401 e é só fazer o login de novo. A troca da própria senha é em `PUT /auth/password`. As decisões estão no [ADR 003](docs/adr/003-authentication.md).

As rotas do cliente (`/customer/serviceOrders/{publicId}`) não usam token: o cliente informa o `publicId` da OS na URL e o próprio CPF/CNPJ no header `X-Customer-Document`. O `publicId` vem na resposta da abertura da OS.

O login e as demais rotas têm limite de requisições (`security.rate-limit` no `application.yml`); acima dele, a API responde 429 com o header `Retry-After`. O limite usa o IP do cliente: atrás de um proxy reverso, o `X-Forwarded-For` só é aceito de endereços privados. Se o proxy usar outro endereço, configure `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` com ele.

## Documentação da API

Com a aplicação no ar, a documentação (Swagger UI) fica em <http://localhost:8080/docs>, com todas as rotas, os contratos, exemplos de requisição e resposta e o formato dos erros ([ADR 002](docs/adr/002-api-errors.md)). A especificação OpenAPI em JSON está em <http://localhost:8080/docs/openapi.json>.

- **Login pela UI:** execute o `POST /auth/login` com o corpo de exemplo. O token da resposta é aplicado sozinho no **Authorize** e continua lá depois de recarregar a página.
- **Fluxo completo:** a descrição no topo da página traz um roteiro que leva uma OS da abertura à entrega e às métricas, só com os exemplos de cada rota, incluindo os caminhos de rejeição, orçamento complementar e cancelamento.
- **Postman:** a collection é gerada pelo import do OpenAPI. O passo a passo está no [README do Postman](docs/postman/README.md).

## Notificações por e-mail

O cliente recebe um e-mail quando o orçamento é enviado, aprovado ou rejeitado e quando a OS é finalizada.

- **Nada se perde:** a notificação é gravada na tabela `email_notification` na mesma transação da ação. Se a gravação falhar, a ação também é desfeita.
- **Envio:** um job envia os pendentes a cada 30 s. Se o envio falhar, ele tenta de novo, cada vez mais espaçado (1 min, 2 min, …), até 5 tentativas; depois disso o e-mail fica como `FAILED`, com o último erro. Cliente sem e-mail também fica como `FAILED`.
- **Textos:** assunto e corpo de cada tipo ficam em `src/main/resources/notifications_pt_BR.properties`.

### Mailpit (padrão local)

O `bootRun` e o `docker compose up` sobem o [Mailpit](https://mailpit.axllent.org/), um servidor SMTP de teste que **não entrega nada de verdade**: ele guarda os e-mails e mostra numa caixa de entrada em <http://localhost:8025>. Não precisa de nenhuma configuração.

### Gmail (envio real)

Para enviar de verdade pelo Gmail:

1. Ative a [verificação em duas etapas](https://myaccount.google.com/signinoptions/twosv) na conta Google.
2. Gere uma [senha de app](https://myaccount.google.com/apppasswords). São 16 letras; use-a no lugar da senha da conta.
3. Defina as variáveis abaixo, no `.env` da raiz (fora do git) para o `docker compose up`, ou nas variáveis de ambiente da IDE ou do terminal para o `bootRun`:

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=sua.conta@gmail.com
MAIL_PASSWORD=senha-de-app-sem-espacos
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_FROM=sua.conta@gmail.com
```

O Gmail limita o envio a cerca de 500 e-mails por dia por conta e reescreve o remetente para a conta autenticada. Nunca faça commit da senha de app.

## Métricas de tempo da OS

As métricas saem do histórico de status de cada OS. A consulta e o índice estão no [ADR 008](docs/adr/008-metrics-queries.md).

- **Quais OS entram:** as que entraram em Finalizada (`COMPLETED`) dentro do período, contado em dias inteiros no fuso da oficina (`America/Sao_Paulo`), com o início e o fim inclusos. Ficam de fora as canceladas e as que pararam em Orçamento rejeitado. Uma OS rejeitada, reaberta e depois executada entra normalmente.
- **Tempo em um status:** soma de todos os períodos que a OS passou nele. Cada entrada no status conta até a mudança de status seguinte. O status atual da OS não conta, porque o período dele ainda não terminou: o tempo em Finalizada só aparece para as OS já entregues.
- **Tempo de execução:** tempo em Em execução (`IN_PROGRESS`). Com o orçamento complementar, a OS entra em execução mais de uma vez, e os períodos se somam. A espera pela aprovação do cliente e a fila até o mecânico iniciar (Orçamento aprovado e Complementar aprovado) ficam de fora.
- **Média:** por status, entre as OS que passaram por ele, junto com a quantidade dessas OS. Sem OS no período, a média é nula.
- **Por serviço do catálogo:** o tempo de cada serviço vai do início à conclusão dele na OS (`POST /admin/serviceOrders/{id}/services/{itemId}/start` e `/complete`), contando só os períodos em Em execução. A média é por unidade, dividida pela soma das quantidades, e fica comparável com o tempo estimado. Junto vêm a quantidade de OS que incluíram o serviço e a de execuções. Nome e tempo estimado são os atuais do catálogo.
- **Rota:** `GET /admin/metrics/averageExecutionTime?from=2026-10-01&to=2026-10-31`, só para ADMIN, com no máximo 366 dias. Cada duração sai em minutos inteiros e em `HH:mm`, com as horas sem limite (`27:05`).

## Testes

Precisa do JDK 25 e do Docker rodando.

```bash
# Compila, roda todos os testes, gera o relatório de cobertura e verifica o mínimo de cobertura
./gradlew build

# Só os testes (unitários, de API e de integração)
./gradlew test

# Só os testes de integração (aceita --tests para filtrar uma classe)
./gradlew integrationTest
```

No fim, o Gradle mostra o resumo (`Tests: X run, Y passed, Z failed`), e o relatório de cada teste fica em `build/reports/tests/test/index.html`.

| Tipo | Como roda | O que cobre |
|---|---|---|
| Unitários | JUnit puro, sem Spring e sem banco | Regras de negócio do domínio e casos de uso |
| API | `@WebMvcTest`, com o caso de uso simulado | Controllers, validação e respostas de erro |
| Integração | `@IntegrationTest`: a aplicação inteira contra um PostgreSQL real via Testcontainers | Fluxos completos, segurança, persistência, migrations e envio de e-mail |
| Arquitetura | ArchUnit (`LayerDependencyTest`) | Isolamento entre módulos e regra entre camadas |

Os padrões de cada tipo estão no [CONTRIBUTING.md](CONTRIBUTING.md#testes). O CI roda o `./gradlew build` em todo PR e em todo push na `main`, e o check é obrigatório para o merge.

### Cobertura de testes

O `./gradlew build` falha se a cobertura de linhas das camadas `domain` e `application` ficar abaixo de 90%. A conta junta essas camadas de todos os módulos num número só, e o mínimo fica em `coverageLineMinimum` no `gradle.properties`.

- **O que entra:** tudo que está em `domain` e `application`: entidades e value objects, use cases, listeners, commands, queries e results.
- **O que fica fora:** `api` (controllers e DTOs HTTP), `infrastructure` (adapters, repositórios JPA, configurações) e a classe `main`. Não têm regra de negócio e já são exercitados pelos testes de integração.

Onde ver a cobertura:

- **Local:** o relatório completo fica em `build/reports/jacoco/test/html`, gerado a cada build e fora do git.
- **CI:** cada run publica o relatório no artefato `coverage-report`, que expira junto com os artefatos do GitHub Actions. Cada PR recebe um comentário com a cobertura por módulo, atualizado a cada push. Nada disso fica no repositório.
- **Entrega:** a evidência versionada está em [docs/evidencias/cobertura](docs/evidencias/cobertura): a tabela por módulo, com data, commit de origem e link da run, e o CSV do JaCoCo com a cobertura por classe. Ela não se atualiza sozinha. Para gerar uma nova, rode o workflow **Coverage evidence** na aba Actions e escolha a branch. Ele roda a suíte, commita a evidência assinada pelo GitHub numa branch nova e deixa no resumo da run o link para abrir o PR. O motivo está no [ADR 010](docs/adr/010-coverage-evidence.md).

## Análise de vulnerabilidades

O código passa por análise estática no SonarQube, rodando localmente. O resultado da última análise, a justificativa de cada achado e o passo a passo para rodar a análise estão em [docs/security/vulnerability-report.md](docs/security/vulnerability-report.md).

## Documentação do projeto

### Decisões de arquitetura

- [ADR 001: Banco de dados e persistência](docs/adr/001-database.md)
- [ADR 002: Padrão de erros da API](docs/adr/002-api-errors.md)
- [ADR 003: Autenticação e autorização](docs/adr/003-authentication.md)
- [ADR 004: Eventos de domínio](docs/adr/004-domain-events.md)
- [ADR 005: Acesso do cliente à ordem de serviço](docs/adr/005-customer-access.md)
- [ADR 006: Orçamento complementar durante a execução](docs/adr/006-supplementary-quote.md)
- [ADR 007: Versionamento de migrations por timestamp e execução fora de ordem](docs/adr/007-timestamp-migrations.md)
- [ADR 008: Consultas de métricas sobre tabelas de outros módulos](docs/adr/008-metrics-queries.md)
- [ADR 009: Reserva de estoque no orçamento](docs/adr/009-stock-reservation.md)
- [ADR 010: Regra de cobertura e evidência versionada](docs/adr/010-coverage-evidence.md)

### Domain-Driven Design

> A definir: link da documentação DDD (Event Storming, contextos delimitados e linguagem ubíqua).

### Vídeo de apresentação

> A definir: link do vídeo de demonstração.

## Contribuindo

Leia o [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir o primeiro PR.
