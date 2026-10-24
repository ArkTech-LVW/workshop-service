# Guia de contribuição

Convenções combinadas para que todos codem no mesmo padrão.

## Idioma

- **Todo o código é escrito em inglês**: classes, métodos, variáveis, enums, rotas, tabelas, colunas e chaves de mensagem.
- A única exceção são os **textos** dos arquivos de i18n (`messages_pt_BR.properties`).
- Commits, descrições de PR e documentação ficam a critério de quem escreve.

## Estrutura de pacotes

O pacote base é `br.com.arktech.workshop`, organizado **por módulo**, com as camadas dentro de cada módulo:

```
br.com.arktech.workshop
└── <module>              ex.: customer, vehicle, serviceorder
    ├── api               controllers e DTOs de entrada/saída
    ├── application       casos de uso, orquestração e transações
    ├── domain            entidades, regras de negócio e interfaces de repositório
    └── infrastructure    implementações técnicas (persistência, integrações)
```


### Regra de dependência entre camadas

```
api ──> application ──> domain <── infrastructure
```

- `domain` **não importa** `api`, `application`, `infrastructure`, Spring (`org.springframework..`), Hibernate (`org.hibernate..`) nem JPA (`jakarta.persistence..`).
- `application` não importa `api` nem `infrastructure`.
- `api` não importa `infrastructure`.
- Entidades JPA (`@Entity`) e repositórios Spring Data (`JpaRepository` etc.) só existem na `infrastructure`.

Essas regras são verificadas pelo teste ArchUnit `LayerDependencyTest`, e uma violação **quebra o build**.

## Mensagens e i18n

- **Nunca escreva texto de mensagem direto no código.** Toda mensagem exibida ao cliente da API vem do `MessageSource`, sempre via `MessageResolver` (`get(code)` ou `get(code, args...)`), que já aplica o locale da aplicação.
- Chaves em inglês no formato `<context>.<camelCase>` (ex.: `customer.notFound`) e textos em `src/main/resources/messages_pt_BR.properties`.
- Ao adicionar um idioma, crie `messages_<locale>.properties`.
- O locale da aplicação é fixo em `pt_BR` (`spring.web.locale`), dentro e fora de requisições HTTP.
- Chave inexistente lança `NoSuchMessageException`; o código da chave nunca é devolvido como mensagem.

## API

- Rotas no plural e em camelCase: `/admin/serviceOrders`.
- DTOs como `record`.
- Datas em ISO-8601.
- Valores monetários em `BigDecimal`.

## Erros da API

Os motivos estão no [ADR 002](docs/adr/002-api-errors.md).

### Formato

Toda resposta de erro é um `ApiError` em `application/json`:

```json
{ "code": "customer.notFound", "message": "Cliente não encontrado.", "details": null }

{ "code": "request.invalidFields", "message": "Existem campos inválidos na requisição.",
  "details": [ { "field": "vehicles[0].plate", "message": "Não pode estar em branco." } ] }
```

- `code` é a chave de i18n e é o que o front usa para decidir o comportamento. Nunca mude o `code` de um erro existente.
- `details` só aparece nos erros de entrada (400); nos demais é `null`.
- Um erro inesperado sai como 500 `server.unexpectedError`, com um código de referência na mensagem. O mesmo código aparece no log `ERROR`, junto com o stack trace.

### Como lançar erros

O domínio e a `application` lançam as exceções de `shared/domain/exception`, sem pensar em HTTP:

| Exceção | Status | Quando |
|---|---|---|
| `NotFoundException` | 404 | O recurso pedido não existe |
| `ConflictException` | 409 | Conflita com o estado atual (ex.: CPF ou placa já cadastrados) |
| `BusinessRuleException` | 422 | Viola uma regra de negócio (ex.: transição de status inválida) |

```java
throw new NotFoundException("customer.notFound", id);
```

- A exceção recebe a chave da mensagem e os argumentos (`{0}`, `{1}`...). Cadastre a chave no `messages_pt_BR.properties`.
- Crie uma subclasse quando o caso se repetir ou quando um nome de negócio deixar o código mais claro: `class InvalidStatusTransitionException extends BusinessRuleException`. O status vem da categoria.
- Não crie `@ExceptionHandler` nos controllers: o `GlobalExceptionHandler` já trata tudo.

### Como documentar os erros

A documentação da API (`/docs`) é gerada pelo springdoc a partir dos controllers. Os erros já têm respostas prontas, com schema e exemplo, no `OpenApiConfig`: `BadRequest` (400), `NotFound` (404), `Conflict` (409) e `BusinessRule` (422). Cada endpoint declara só os erros que de fato pode retornar:

```java
@Operation(summary = "${openapi.customer.findById}")
@ApiResponse(responseCode = "200")
@ApiResponse(responseCode = "404", ref = "NotFound")
@GetMapping("/{id}")
CustomerResponse findById(@PathVariable Long id) { ... }
```

```properties
# src/main/resources/openapi/openapi.properties
openapi.customer.findById=Busca um cliente
```

- Os textos da documentação (`summary`, `description`) não ficam no código: vão para o `openapi/openapi.properties`, com chaves `openapi.<context>.<camelCase>`, e são referenciados com `${...}`. Esse arquivo é só da documentação; as mensagens devolvidas ao cliente continuam no `messages_pt_BR.properties`.
- Uma chave inexistente aparece crua na doc (`${openapi.customer.findById}`) e quebra o `OpenApiDocsTest`.
- O 500 (`InternalError`) entra sozinho em todas as operações.
- Toda rota exige o token JWT na documentação. Rota pública desliga o requisito com `@SecurityRequirements()` no método ou na classe.

### Mensagens de validação

- As anotações de Bean Validation usam a mensagem padrão (`jakarta.validation.constraints.NotBlank.message`) ou uma chave própria entre chaves: `@NotBlank(message = "{customer.nameRequired}")`. Texto literal é proibido.
- Ao usar uma anotação pela primeira vez, cadastre a mensagem dela no `messages_pt_BR.properties`. Uma mensagem literal ou uma chave inexistente lança exceção na validação (500), então o teste que dispara a validação acusa o esquecimento.
- Parâmetros da anotação entram como `{min}`, `{max}`, `{value}`: `Deve ter entre {min} e {max} caracteres.`
- Em mensagens com argumentos (`{0}`), o apóstrofo precisa ser duplicado (`''`), porque o `MessageFormat` trata `'` como escape. Mensagens de validação e mensagens sem argumentos usam o apóstrofo simples.

## Banco de dados e persistência

PostgreSQL 18 com Spring Data JPA e Flyway. Os motivos estão no [ADR 001](docs/adr/001-database.md).

A imagem do banco tem versão fixa (`postgres:18.6-alpine`) e aparece em dois lugares: `docker-compose.yml` (desenvolvimento) e `TestcontainersConfiguration` (testes). Ao atualizar a versão, mude os dois.

### Migrations

- O esquema vem **só das migrations** (`ddl-auto=validate`): toda tabela, coluna ou índice novo entra por uma migration em `src/main/resources/db/migration`.
- Nome no padrão `V<n>__<description>.sql`, com a descrição em inglês e snake_case (ex.: `V2__create_customer.sql`). Use o próximo número livre na `main`; se outro PR mergear a mesma versão antes do seu, renumere a sua.
- **Nunca altere uma migration que já está na `main`.** Para corrigir, crie uma nova.
- Tabelas no singular e colunas em inglês e snake_case (`service_order`, `created_at`).
- Chave primária: `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`.

### Entidades e repositórios

O domínio não sabe nada de banco. Cada módulo tem a classe de domínio e a interface do repositório no `domain`, e na `infrastructure` a entidade JPA, a interface Spring Data e um adapter que liga as duas:

```java
// <module>/domain
@Getter
public class Customer {

    private final Long id;
    private String name;

    public Customer(String name) {
        this(null, name);
    }

    public Customer(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(Long id);
}

// <module>/infrastructure
@Entity
@Table(name = "customer")
@Getter
class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    protected CustomerEntity() {}

    CustomerEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}

interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {}

@Component
@RequiredArgsConstructor
class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpaRepository;

    @Override
    public Customer save(Customer customer) {
        return toDomain(jpaRepository.save(toEntity(customer)));
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private Customer toDomain(CustomerEntity entity) {
        return new Customer(entity.getId(), entity.getName());
    }

    private CustomerEntity toEntity(Customer customer) {
        return new CustomerEntity(customer.getId(), customer.getName());
    }
}
```

- A classe de domínio tem dois construtores: `Customer(name)` para um objeto novo e `Customer(id, name)` para um objeto vindo do banco.
- O mapeamento é feito à mão, em `toDomain`/`toEntity` no adapter. Se crescer, extraia para uma classe `CustomerMapper` na `infrastructure`.
- Entidade JPA, interface Spring Data e adapter ficam package-private: fora da `infrastructure`, ninguém os enxerga. O construtor `protected` sem argumentos da entidade existe porque o JPA exige.
- Consultas novas entram na interface do domínio com nome de negócio; o adapter delega para um método derivado ou um `@Query` no `CustomerJpaRepository`.
- O objeto de domínio não é acompanhado pelo Hibernate: toda alteração é gravada com `save` explícito.
- A `application` injeta sempre a interface do domínio (`CustomerRepository`), e as transações (`@Transactional`) ficam nos casos de uso dela.

## Lombok

- **Liberado:** `@RequiredArgsConstructor`, `@Getter`, `@Slf4j` e `@Builder` quando fizer sentido.
- **Proibido:** `@Data` e `@Value`. Para dados imutáveis, use `record`. O `lombok.config` faz a compilação falhar se eles forem usados.
- Injeção de dependência **sempre por construtor** (campos `final` + `@RequiredArgsConstructor`), nunca `@Autowired` em campo.
- Entidades JPA: sem `@EqualsAndHashCode` e `@ToString` gerados. Escreva `equals`/`hashCode` à mão, baseados no id.

## Nulabilidade

Nada aceita `null`, a não ser o que está marcado com `@Nullable` (`org.jspecify.annotations`, do [JSpecify](https://jspecify.dev)). A regra é verificada na compilação pelo [NullAway](https://github.com/uber/NullAway).

- **Vale para todo o código de produção**, sem anotar classe ou pacote: o NullAway está configurado com `AnnotatedPackages=br.com.arktech.workshop`, que cobre o pacote base e todos os subpacotes. Classes e módulos novos já entram na verificação.
- Marque com `@Nullable` só o que de fato pode ser nulo: campo, parâmetro, retorno ou componente de record.

```java
public record ApiFieldError(@Nullable String field, String message) {}
```

- **Variável local não leva `@Nullable`**: o JSpecify não aceita a anotação ali, e o NullAway infere a nulabilidade da variável pelo fluxo do código.
- Na sobrescrita de um método do Spring, repita o `@Nullable` dos parâmetros e do retorno da classe-mãe (ex.: `handleExceptionInternal` no `GlobalExceptionHandler`).
- O `./gradlew build` falha quando um valor `@Nullable` é usado sem checagem, ou quando `null` é passado onde não é aceito. Os testes não são verificados.

## Dependências

- Adicione uma dependência **somente na tarefa que precisar dela**, nunca "para usar depois".
- Versões ficam no version catalog (`gradle/libs.versions.toml`). Dependências gerenciadas pelo Spring Boot entram sem versão.

## Testes

- Testes automatizados com **JUnit**.
- `./gradlew build` roda todos os testes e precisa passar antes de abrir o PR.
- **Unitários:** JUnit puro, sem Spring e sem banco. É o padrão para domínio e regras de negócio.
- **Camada `api`:** `@WebMvcTest(controllers = ...)` é o padrão para testar controllers, validação e respostas de erro sem subir banco. Veja o `GlobalExceptionHandlerTest`.
- **Integração:** anote a classe com `@IntegrationTest` (ou `@IntegrationTest(webEnvironment = RANDOM_PORT)` para chamar via HTTP). A anotação sobe a aplicação inteira contra um PostgreSQL real (Testcontainers) e marca o teste com a tag JUnit `integration`. O Docker precisa estar rodando.
- Use `@IntegrationTest` só quando o teste precisa da aplicação ou do banco de verdade. Testes com a mesma configuração compartilham contexto e container; cada configuração diferente (ex.: outro `webEnvironment`) sobe os seus.
- **Cobertura:** o `./gradlew build` também gera o relatório do JaCoCo em `build/reports/jacoco/test/html/index.html`, mesmo quando algum teste falha.

## Pull Requests

- Todo PR é aberto para a `main` usando o template do repositório, com o link da tarefa no Jira.
- O merge é feito com **merge commit**.

### CI

- O workflow `.github/workflows/ci.yml` roda `./gradlew build` (compilação, testes unitários e de integração, cobertura) em todo PR e em todo push na `main`, com JDK 25 (Temurin) e cache do Gradle. O runner do GitHub já vem com Docker, então os testes de integração sobem o PostgreSQL via Testcontainers como na sua máquina.
- O check `build` é obrigatório na `main`: PR com build ou teste quebrado não pode ser mergeado.
- Quando algum teste falha num PR, o CI comenta no PR com o resumo (`Tests: X run, Y passed, Z failed`), os testes que falharam e o link do log.
- O relatório de cobertura fica no artefato `coverage-report` de cada execução (página da execução no GitHub Actions, seção *Artifacts*).
