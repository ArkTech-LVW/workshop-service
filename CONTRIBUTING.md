# Guia de contribuição

Convenções combinadas para que todos codem no mesmo padrão.

## Idioma

- **Todo o código é escrito em inglês**: classes, métodos, variáveis, enums, rotas, tabelas, colunas e chaves de mensagem.
- A única exceção são os **textos** dos arquivos de i18n (`messages_pt_BR.properties`).
- Commits, descrições de PR e documentação ficam a critério de quem escreve.

## Comentários

- **O código não leva comentários.** Nomes de classes, métodos e variáveis explicam o que o código faz, e o motivo de uma decisão vai para o ADR, o PR ou o commit.
- Isso vale também para Javadoc e para comentários nos arquivos de configuração (`application.yml`).
- Se o código precisa de comentário para ser entendido, melhore os nomes ou extraia um método antes.

## Estrutura de pacotes

O pacote base é `br.com.arktech.workshop`, organizado **por módulo**, com as camadas dentro de cada módulo e subpacotes dentro das camadas:

```
br.com.arktech.workshop
├── shared                    o que todos os módulos usam (erros, i18n, segurança, BaseEntity, UseCase)
└── <module>                  ex.: auth, registry, serviceorder
    ├── api                   controllers e DTOs de entrada/saída
    │   └── <recurso>         um pacote por controller, com os DTOs dele (ex.: user, authentication)
    ├── application           casos de uso, orquestração e transações
    │   ├── <caso de uso>     um pacote por caso de uso (ex.: login, createuser)
    │   ├── <objeto>          result usado por vários casos de uso (ex.: user/UserResult)
    │   └── port              interfaces que os casos de uso usam e a infraestrutura implementa
    ├── domain                entidades, regras de negócio e contratos
    │   ├── model             entidades, value objects e enums
    │   ├── repository        interfaces de repositório
    │   └── port              outras interfaces que o domínio usa e a infraestrutura implementa
    └── infrastructure        implementações técnicas
        ├── config            @Configuration e @ConfigurationProperties
        ├── persistence       interfaces Spring Data
        └── <assunto>         demais implementações, agrupadas por assunto (ex.: security)
```

- **Port** é uma interface que a camada declara para dizer do que precisa, sem saber como é feito (ex.: `PasswordHasher` no domínio, `TokenIssuer` na `application`). A implementação fica na `infrastructure`. Repositórios também são ports, mas ficam no próprio `repository`.
- O pacote de um caso de uso tem o nome dele em minúsculas, sem separador (`createuser`), e guarda o `XxxUseCase`, o `XxxCommand`/`XxxQuery` e o `XxxResult`. Quando vários casos de uso devolvem o mesmo objeto, o result fica num pacote com o nome do objeto (`user/UserResult`).
- Na `api`, cada controller fica num pacote com o nome do recurso (`user`, `authentication`), junto com os DTOs que só ele usa.
- Crie o subpacote junto com o primeiro arquivo dele; não deixe pacotes vazios.
- Classes usadas só dentro do próprio subpacote ficam package-private; as que outro subpacote do módulo usa ficam `public`. O isolamento entre módulos continua garantido pelo `LayerDependencyTest`.
- O `shared` segue a mesma divisão em camadas, com subpacotes por assunto (`error`, `i18n`, `exception`...). A exceção é o `shared/event`, com os eventos de domínio, que fica fora das camadas porque o `domain` do módulo dono também o importa.

### Isolamento entre módulos

- Um módulo só importa classes dele mesmo e do `shared`, nunca de outro módulo.
- A referência a algo de outro módulo é pelo id: `Long createdByUserId`, nunca `User createdBy` nem `@ManyToOne` para entidade de outro módulo. A FK continua no banco.
- O que um módulo precisa de outro vira um contrato (interface) no `shared`, implementado pelo módulo dono. Ex.: `CurrentUser` (`shared/application/security`), implementado no `auth`. Crie o contrato só quando algum módulo precisar dele.
- Para avisar outro módulo de um fato, o agregado registra um evento cujo `record` fica no `shared/event/<módulo dono>` (ex.: `QuoteApprovedEvent`, em `shared/event/serviceorder`), e o outro módulo o escuta com `@EventListener` (ver [Entidades e repositórios](#entidades-e-repositórios)).

### Regra de dependência entre camadas

```
api ──> application ──> domain <── infrastructure
```

- `domain` **não importa** `api`, `application`, `infrastructure`, Spring (`org.springframework..`) nem Hibernate (`org.hibernate..`), com duas exceções: as anotações `org.hibernate.annotations` e as anotações de eventos do Spring Data (`@DomainEvents` e `@AfterDomainEventPublication`, [ADR 004](docs/adr/004-domain-events.md)). JPA (`jakarta.persistence`) é liberado, porque as entidades são as próprias classes de domínio ([ADR 001](docs/adr/001-database.md)).
- `application` não importa `api` nem `infrastructure`.
- `api` não importa `infrastructure`.
- Entidades JPA (`@Entity`, `@MappedSuperclass`) ficam no `domain`; repositórios Spring Data (`JpaRepository` etc.) só na `infrastructure`.

Essas regras, o isolamento entre módulos e os padrões de controller e de caso de uso (abaixo) são verificados pelo teste ArchUnit `LayerDependencyTest`, e uma violação **quebra o build**.

## Casos de uso

Cada caso de uso é uma classe da `application`, no pacote do próprio caso de uso, que implementa `UseCase<I, O>` (`shared/application`), com um único método `execute`:

```java
@Service
@RequiredArgsConstructor
public class FindCustomerUseCase implements UseCase<FindCustomerQuery, CustomerResult> {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomerResult execute(FindCustomerQuery query) { ... }
}
```

- Nome: `<Ação><Objeto>UseCase` (`LoginUseCase`, `CreateCustomerUseCase`).
- Entrada e saída são `record`s da `application`, separados dos DTOs da `api`: `XxxCommand` para o que altera estado, `XxxQuery` para consultas e `XxxResult` para a saída. O controller converte o DTO de entrada em command/query e o result no DTO de resposta.
- Caso de uso sem saída implementa `UseCase<XxxCommand, @Nullable Void>` e devolve `null` (ex.: `ChangePasswordUseCase`).
- `@Transactional` fica no `execute` (`readOnly = true` nas consultas).
- O controller injeta a classe do caso de uso diretamente.
- Record com senha ou outro dado sensível sobrescreve o `toString` para mascarar o valor, porque o `toString` padrão do record imprime todos os campos.

## Mensagens e i18n

- **Nunca escreva texto de mensagem direto no código.** Toda mensagem exibida ao cliente da API vem do `MessageSource`, sempre via `MessageResolver` (`get(code)` ou `get(code, args...)`), que já aplica o locale da aplicação.
- Chaves em inglês no formato `<context>.<camelCase>` (ex.: `customer.notFound`) e textos em `src/main/resources/messages_pt_BR.properties`. Os textos das notificações (`notification.*`) ficam em `src/main/resources/notifications_pt_BR.properties`.
- Ao adicionar um idioma, crie `messages_<locale>.properties` e `notifications_<locale>.properties`.
- O locale da aplicação é fixo em `pt_BR` (`spring.web.locale`), dentro e fora de requisições HTTP. O `MessageResolver` usa sempre esse locale, inclusive nos filtros do Spring Security, que rodam antes do `DispatcherServlet` ([ADR 002](docs/adr/002-api-errors.md)).
- Chave inexistente lança `NoSuchMessageException`; o código da chave nunca é devolvido como mensagem.

## API

- Rotas no plural e em camelCase: `/admin/serviceOrders`.
- DTOs como `record`.
- Datas em ISO-8601.
- Valores monetários em `BigDecimal`.
- Criação responde `201` com o recurso criado; ação sem corpo de resposta responde `204`.

### Paginação

Listagens recebem `page` (começa em 0, padrão 0) e `size` (de 1 a 100, padrão 20) como query params e devolvem um `PageResponse` (`shared/api/pagination`):

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 42, "totalPages": 3 }
```

- O repositório do domínio recebe `Pagination` e devolve `Page<T>` (`shared/domain/pagination`), sem Spring Data. O `XxxJpaRepository` converte num método `default` (ver `UserJpaRepository.findPage`).
- Toda listagem tem ordem estável: ordene por um campo de negócio e desempate pelo `id`.
- O controller converte com `PageResponse.from(page, XxxResponse::from)`.

### Controllers

As anotações de rota, validação e documentação ficam numa interface `I<Nome>Controller`; o controller só a implementa:

```java
@Tag(name = "${openapi.customer.tag}")
@RequestMapping("/admin/customers")
interface ICustomerController {

    @AllowedRoles({Role.ADMIN, Role.ATTENDANT})
    @Operation(summary = "${openapi.customer.findById}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @GetMapping("/{id}")
    CustomerResponse findById(@PathVariable Long id);
}

@RestController
@RequiredArgsConstructor
class CustomerController implements ICustomerController {

    private final FindCustomerUseCase findCustomerUseCase;

    @Override
    public CustomerResponse findById(Long id) {
        return CustomerResponse.from(findCustomerUseCase.execute(new FindCustomerQuery(id)));
    }
}
```

- Na classe ficam só `@RestController` e `@RequiredArgsConstructor`, sem anotação nos métodos nem nos parâmetros: o Spring lê `@PathVariable`, `@RequestBody`, `@Valid` etc. da interface.
- Todo endpoint declara quem pode acessá-lo, também na interface (ver [Segurança](#segurança)). Sem declaração, a rota fica bloqueada para todo mundo.
- Constraints de validação nos parâmetros (`@Positive`, `@NotNull`...) ficam **só** na interface. Repetir na classe faz o Hibernate Validator lançar `ConstraintDeclarationException` (HV000151).
- Interface e controller ficam package-private, na `api` do módulo.

### Postman

A collection do Postman é gerada pelo import do OpenAPI (`/docs/openapi.json`), então não há arquivo para manter. Para a rota sair pronta para uso, dê `example` a cada campo do corpo e a cada parâmetro (`@Schema(example = ...)` e `@Parameter(example = ...)`): sem ele, o Postman preenche com texto aleatório. Como importar e usar está no [README do Postman](docs/postman/README.md).

## Erros da API

Os motivos estão no [ADR 002](docs/adr/002-api-errors.md).

### Formato

Toda resposta de erro é um `ApiError` em `application/json`:

```json
{ "code": "customer.notFound", "message": "Cliente 1234 não encontrado." }

{ "code": "request.invalidFields", "message": "Existem campos inválidos na requisição.",
  "details": [ { "field": "vehicles[0].plate", "message": "Não pode estar em branco." } ] }
```

- `code` é a chave de i18n e é o que o front usa para decidir o comportamento. Nunca mude o `code` de um erro existente.
- `details` só aparece nos erros de entrada (400); nos demais, o campo não vem na resposta. Os campos saem sempre na ordem `code`, `message`, `details`.
- Um erro inesperado sai como 500 `server.unexpectedError`, com um código de referência na mensagem. O mesmo código aparece no log `ERROR`, junto com o stack trace.

### Como lançar erros

O domínio e a `application` lançam as exceções de `shared/domain/exception`, sem pensar em HTTP:

| Exceção | Status | Quando |
|---|---|---|
| `NotFoundException` | 404 | O recurso pedido não existe |
| `ConflictException` | 409 | Conflita com o estado atual (ex.: CPF ou placa já cadastrados) |
| `BusinessRuleException` | 422 | Viola uma regra de negócio (ex.: transição de status inválida) |
| `UnauthorizedException` | 401 | O próprio caso de uso recusa a identificação (ex.: login com credenciais inválidas) |
| `TooManyRequestsException` | 429 | Passou do limite de tentativas (ex.: login por e-mail + IP). Recebe o tempo até liberar, que sai no header `Retry-After` |

O 401 e o 403 de token ausente, inválido ou sem o papel exigido saem do Spring Security, não dessas exceções (ver [Segurança](#segurança)).

```java
throw new NotFoundException("customer.notFound", id.toString());
```

- A exceção recebe a chave da mensagem e os argumentos (`{0}`, `{1}`...). Cadastre a chave no `messages_pt_BR.properties`.
- Ids e outros números que não são quantidade entram como `String` (`id.toString()`): o `MessageFormat` formata números no locale, e o id 1234 sairia como `1.234`.
- Crie uma subclasse quando o caso se repetir ou quando um nome de negócio deixar o código mais claro: `class InvalidStatusTransitionException extends BusinessRuleException`. O status vem da categoria.
- Não crie `@ExceptionHandler` nos controllers: o `GlobalExceptionHandler` já trata tudo.

### Como documentar os erros

A documentação da API (`/docs`) é gerada pelo springdoc a partir das interfaces dos controllers. Os erros já têm respostas prontas, com schema e exemplo, no `OpenApiConfig`: `BadRequest` (400), `InvalidCredentials` (401 do login), `NotFound` (404), `Conflict` (409) e `BusinessRule` (422), além do `Unauthenticated` (401), do `AccessDenied` (403) e do `TooManyRequests` (429), que entram sozinhos (ver abaixo). Cada endpoint declara só os erros que de fato pode retornar, como o `findById` do exemplo de [Controllers](#controllers):

```java
@ApiResponse(responseCode = "200")
@ApiResponse(responseCode = "404", ref = "NotFound")
```

```properties
# src/main/resources/openapi/openapi.properties
openapi.customer.findById=Busca um cliente
```

- Os textos da documentação (`summary`, `description`) não ficam no código: vão para o `openapi/openapi.properties`, com chaves `openapi.<context>.<camelCase>`, e são referenciados com `${...}`. Esse arquivo é só da documentação; as mensagens devolvidas ao cliente continuam no `messages_pt_BR.properties`.
- Uma chave inexistente aparece crua na doc (`${openapi.customer.findById}`) e quebra o `OpenApiDocsTest`.
- O 429 (`TooManyRequests`) e o 500 (`InternalError`) entram sozinhos em todas as operações.
- O cadeado e as respostas de segurança saem da anotação de acesso do endpoint: `@PublicEndpoint` fica sem cadeado; `@Authenticated` ganha o 401 (`Unauthenticated`); `@AllowedRoles` ganha o 401 e o 403 (`AccessDenied`). Não declare esses três à mão.

### Mensagens de validação

- As anotações de Bean Validation usam a mensagem padrão (`jakarta.validation.constraints.NotBlank.message`) ou uma chave própria entre chaves: `@NotBlank(message = "{customer.nameRequired}")`. Texto literal é proibido.
- Ao usar uma anotação pela primeira vez, cadastre a mensagem dela no `messages_pt_BR.properties`. Uma mensagem literal ou uma chave inexistente lança exceção na validação (500), então o teste que dispara a validação acusa o esquecimento.
- Parâmetros da anotação entram como `{min}`, `{max}`, `{value}`: `Deve ter entre {min} e {max} caracteres.`
- `@Size` só com `max` usa `message = "{validation.maxLength}"` (`Deve ter no máximo {max} caracteres.`); a mensagem padrão diria "entre 0 e {max}".
- Em mensagens com argumentos (`{0}`), o apóstrofo precisa ser duplicado (`''`), porque o `MessageFormat` trata `'` como escape. Mensagens de validação e mensagens sem argumentos usam o apóstrofo simples.

## Segurança

Os motivos estão no [ADR 003](docs/adr/003-authentication.md).

### Acesso às rotas

Nenhuma rota é acessível por padrão. Cada endpoint declara quem pode acessá-lo com uma das anotações de `shared/api/security`, no método ou na interface `I<Nome>Controller` inteira:

| Anotação | Quem acessa |
|---|---|
| `@PublicEndpoint` | Qualquer um, sem token |
| `@Authenticated` | Qualquer usuário logado, de qualquer papel |
| `@AllowedRoles({Role.ADMIN, Role.ATTENDANT})` | Quem tem pelo menos um dos papéis |

```java
@Authenticated
@RequestMapping("/admin/customers")
interface ICustomerController {

    @GetMapping("/{id}")
    CustomerResponse findById(@PathVariable Long id);      // @Authenticated, da interface

    @AllowedRoles(Role.ADMIN)
    @DeleteMapping("/{id}")
    void delete(@PathVariable Long id);                    // a do método vence a da interface
}
```

- **Endpoint sem anotação fica bloqueado para todo mundo, inclusive ADMIN** (403 com token, 401 sem). O `EndpointAccessCoverageTest` percorre todos os endpoints e quebra o build quando algum fica sem declaração.
- Não há hierarquia de papéis: o ADMIN só acessa o que lista `Role.ADMIN`.
- Os papéis vêm sempre do enum `Role`, nunca de string.
- Duas anotações de acesso no mesmo método (ou na mesma interface) e `@AllowedRoles({})` são erro de configuração e também quebram o `EndpointAccessCoverageTest`.
- Não use as anotações do Spring (`@PreAuthorize`, `@Secured`) nem as do Jakarta (`@PermitAll`, `@RolesAllowed`): elas não são lidas e o endpoint continua bloqueado.
- Fora dos controllers, ficam abertos só o `/actuator/health/**` e a documentação (`/docs/**`, `/swagger-ui/**`), na `SecurityConfig`. Qualquer outro caminho, inclusive rota inexistente, é negado. Numa rota existente, a requisição fora do formato (método, `Content-Type`, `Accept`) continua com o erro do MVC (405, 415, 406), mesmo sem token.

### Token e usuário logado

- As rotas protegidas exigem um token JWT no header `Authorization: Bearer <token>`.
- Sem token, ou com token inválido ou expirado: 401 (`auth.unauthenticated` ou `auth.invalidToken`). Com token, mas sem permissão: 403 (`auth.accessDenied`). Os dois saem pelo `GlobalExceptionHandler`, como os demais erros.
- As rotas públicas (`@PublicEndpoint`, `/docs`, `/actuator/health`) ignoram o header `Authorization`: com token inválido ou vencido, respondem como se ele não existisse. Quem decide é o `PublicRouteBearerTokenResolver`, a partir das mesmas regras de acesso da autorização.
- O caso de uso que precisa do IP do cliente injeta `ClientAddress` (`shared/application/security`) e chama `clientAddress.ip()`. Atrás de proxy confiável, já é o IP real (ver [Rate limit](#rate-limit)).
- O caso de uso que precisa registrar quem fez a ação injeta `CurrentUser` (`shared/application/security`) e chama `currentUser.id()`. O id vem do token (`sub`). Os outros módulos guardam esse `Long`, nunca o `User`.
- Senhas com hash Argon2id, pelo `PasswordHasher` do `auth`. Nunca logue nem devolva senha ou hash.
- O token (HS256) leva o id do usuário (`sub`) e os papéis (`roles`) e vale 1h (`security.jwt.expiration`). A chave vem da variável `JWT_SECRET` (mínimo de 32 bytes). Só o perfil `development`, padrão para rodar localmente, traz uma chave de desenvolvimento; em qualquer outro perfil, sem a variável a aplicação não sobe. Os perfis estão no README.

### Rate limit

Os motivos estão no [ADR 003](docs/adr/003-authentication.md#rate-limit).

- Toda rota tem limite, sem anotação: o `RateLimitFilter` conta por usuário logado ou, sem token, por IP. O health fica de fora.
- Rota sem configuração própria usa o limite geral (`security.rate-limit.general`).
- Para dar um limite próprio a uma rota, cadastre-a em `security.rate-limit.routes`, com um nome, o `path` (padrão do Spring MVC, aceita `{id}` e `**`), o `method` (opcional; sem ele, vale para todos) e os valores:

  ```yaml
  security:
    rate-limit:
      routes:
        login:
          method: POST
          path: /auth/login
          capacity: 20
          period: PT1M
  ```

- A rota cadastrada usa só o limite dela, sem gastar o geral. Quando mais de uma casa, vale a mais específica (`/admin/users` antes de `/admin/**`, e com `method` antes de sem).
- Um limite que depende do corpo da requisição não cabe no filtro. Ele vira uma porta na `application` do módulo, implementada na `infrastructure` com o `RateLimit`, que lança `TooManyRequestsException`. É o caso do login por e-mail + IP (`login-per-email-and-ip`, porta `LoginRateLimiter`). Nunca bloqueie a conta nem limite só pelo identificador do usuário (e-mail, CPF): qualquer um conseguiria travar outra pessoa de propósito.
- Os contadores ficam em memória e valem por instância.
- Nos testes de integração, o `application-test.yml` sobe os limites para todos os testes, que chamam do mesmo IP. Para testar o limite, use `@TestPropertySource` com valores baixos e um IP próprio por teste (`request.setRemoteAddr`), como no `RateLimitIntegrationTest`.

## Banco de dados e persistência

PostgreSQL 18 com Spring Data JPA e Flyway. Os motivos estão no [ADR 001](docs/adr/001-database.md).

A imagem do banco tem versão fixa (`postgres:18.6-alpine`) e aparece em dois lugares: `docker-compose.yml` (desenvolvimento) e `TestcontainersConfiguration` (testes). Ao atualizar a versão, mude os dois.

### Migrations

- O esquema vem **só das migrations** (`ddl-auto=validate`): toda tabela, coluna ou índice novo entra por uma migration em `src/main/resources/db/migration`.
- Nome no padrão `V<n>__<description>.sql`, com a descrição em inglês e snake_case (ex.: `V2__create_customer.sql`). Use o próximo número livre na `main`; se outro PR mergear a mesma versão antes do seu, renumere a sua.
- **Nunca altere uma migration que já está na `main`.** Para corrigir, crie uma nova.
- Tabelas no singular e colunas em inglês e snake_case (`service_order`, `created_at`).
- Chave primária: `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` (mapeada na `BaseEntity`).
- Toda tabela tem as colunas da `TimeStamping`: `date_created TIMESTAMPTZ NOT NULL`, `last_updated TIMESTAMPTZ NOT NULL` e `version BIGINT NOT NULL`.
- `user` é palavra reservada no PostgreSQL: nas migrations e em SQL nativo, a tabela é sempre `"user"`, entre aspas. Sem aspas, `SELECT * FROM user` não dá erro: devolve o usuário da conexão.

### Entidades e repositórios

As classes de domínio são as próprias entidades JPA ([ADR 001](docs/adr/001-database.md)). A interface do repositório fica no `domain/repository`, sem Spring, e a interface Spring Data da `infrastructure/persistence` a estende:

```java
// <module>/domain/model
@Entity
@Table(name = "customer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends BaseEntity {

    private String name;

    public Customer(String name) {
        this.name = name;
    }

    public void rename(String name) {
        this.name = name;
    }
}

// <module>/domain/repository
public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(Long id);
}

// <module>/infrastructure/persistence
interface CustomerJpaRepository extends JpaRepository<Customer, Long>, CustomerRepository {}
```

- Toda entidade estende `BaseEntity` (`shared/domain`), que traz o `id` (`IDENTITY`) e o `equals`/`hashCode` pelo id. A `BaseEntity` estende a `TimeStamping`: `dateCreated` e `lastUpdated` são preenchidos pelo Hibernate (`@CreationTimestamp`/`@UpdateTimestamp`) e `version` é o `@Version` do lock otimista. O `LayerDependencyTest` exige que toda `@Entity` estenda a `BaseEntity`.
- O construtor `protected` sem argumentos existe porque o JPA exige e é gerado pelo `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. O construtor de negócio, que cria um objeto novo sem id, é escrito à mão: `@RequiredArgsConstructor` não serve, porque campo de entidade não pode ser `final`, e `@AllArgsConstructor` prenderia o construtor à lista e à ordem dos campos.
- Sem setters: o estado muda por métodos de negócio (`rename`, `deactivate`).
- O Spring Data implementa os métodos do repositório do domínio através do `XxxJpaRepository`, que fica package-private. Consultas novas entram na interface do domínio com nome de negócio; se precisarem de `@Query`, o método é redeclarado com a anotação no `XxxJpaRepository`.
- Filhos do mesmo agregado (ex.: `UserRole` no `User`) são entidades mapeadas a partir da raiz (`@OneToMany(mappedBy = ..., cascade = CascadeType.ALL, orphanRemoval = true)`). Entidade de outro módulo, nunca: guarde o id.
- Cada evento de domínio é um `record` próprio no `shared/event/<módulo dono>`, com o nome terminado em `Event` (ex.: `QuoteApprovedEvent`). Um evento só existe quando tem ouvinte: crie o `record` junto com o primeiro `@EventListener` que o escuta, e remova-o quando o último ouvinte deixar de existir. Ele leva só o que os ouvintes precisam, em tipos simples, e não implementa nenhuma interface: não crie interface selada por agregado nem marcadora. O método de negócio registra o evento numa lista `@Transient` (`List<Object>`) do próprio agregado, exposta por um método `@DomainEvents` e limpa por um `@AfterDomainEventPublication` (ver `ServiceOrder`). O Spring Data publica os eventos no `save`, de forma síncrona e dentro da transação do caso de uso, então o caso de uso não publica nada à mão e **precisa** chamar `save` para os eventos saírem. A exceção é o fato que não passa pelo `save` de um agregado (ex.: uma alteração em lote por consulta): só nesse caso, o caso de uso publica pelo `ApplicationEventPublisher`, depois de gravar, e o PR explica por quê. Um ouvinte síncrono que falha desfaz a transação inteira. Não use `AbstractAggregateRoot`: a entidade já estende a `BaseEntity`. Nos testes unitários, o repositório em memória imita essa publicação. Os motivos estão no [ADR 004](docs/adr/004-domain-events.md).
- **Ouvinte síncrono para o que pode barrar, assíncrono para aviso.** O ouvinte que precisa poder impedir o fato (ex.: a baixa de estoque, que desfaz a aprovação quando falta saldo) usa `@EventListener`: roda dentro da transação do caso de uso, e uma exceção nele desfaz tudo. O ouvinte que só avisa sobre um fato já gravado (ex.: as notificações ao cliente) usa `@Async` com `@TransactionalEventListener(phase = AFTER_COMMIT)`: só roda se a transação for confirmada, numa thread do executor do Spring Boot (`spring.task.execution` no `application.yml`), e captura as próprias exceções, registrando só um `log.error` (ver `ServiceOrderNotificationListener`). Para notificar, o módulo monta o texto com chaves `notification.<módulo>.<camelCase>` (no `notifications_pt_BR.properties`) e envia pelo `Notifier` do `shared/application/notification`; o módulo `notification` só faz a entrega. Nunca use ouvinte síncrono para aviso: uma falha nele desfaria a ação principal.
- Value objects são `record`s com a regra (normalização e validação) no construtor compacto: não existe um `Email` fora do padrão. Quem recebe um `String` de fora cria o value object (`new Email(command.email())`) antes de usar ou consultar.
- Value object de **uma coluna** (ex.: `Email`, em `shared/domain/email`) é persistido por um `AttributeConverter` com `@Converter(autoApply = true)`, ao lado do `record`: a entidade só declara o campo (`private Email email;`) e o repositório recebe o value object (`findByEmail(Email email)`). Value object de **várias colunas** (ex.: um endereço) é `@Embeddable`.
- Valor monetário no domínio é sempre `Money` (`shared/domain/money`), nunca `BigDecimal` solto: tem duas casas decimais, não aceita negativo e faz as contas na própria classe (`plus`, `minus`, `times`), devolvendo um novo `Money`. Na API, entra e sai como `BigDecimal`, e o campo do DTO leva a anotação `@Money` (`shared/api/validation`), que responde 400 (`money.invalid`) para valor negativo, com mais de duas casas decimais ou com mais de 10 dígitos inteiros; `@NotNull` e `@Positive` entram junto quando o valor é obrigatório ou precisa ser maior que zero. No banco, é uma coluna `NUMERIC(12, 2)`.
- A aplicação roda no fuso da oficina (`America/Sao_Paulo`): o `WorkshopApplication.main` define o fuso padrão da JVM, e a task `test` do `build.gradle.kts` faz o mesmo nos testes, que não passam pelo `main`. Mantenha os dois iguais. Assim, `Year.now()` e `LocalDate.now()` já devolvem o ano e o dia da oficina (número da OS, ano máximo do veículo). Momentos no tempo continuam `Instant` em coluna `TIMESTAMPTZ`, que não dependem do fuso.
- A `application` injeta sempre a interface do domínio (`CustomerRepository`), e as transações (`@Transactional`) ficam nos casos de uso. Dentro da transação, o Hibernate grava as alterações sozinho no commit; chame `save` mesmo assim, para deixar a intenção explícita.
- Filtros de listagem (query params opcionais) viram um `XxxFilter` do domínio, com os value objects já criados (ex.: `CustomerFilter`). O `XxxJpaRepository` monta a consulta com `Specification`, só com os filtros informados (ver `CustomerJpaRepository.findPage`).
- A exclusão é física. O repositório do domínio declara `remove(Xxx)`, e o `XxxJpaRepository` o implementa num método `default` que chama `delete` e `flush` e converte a `DataIntegrityViolationException` em `ConflictException` (ver `CustomerJpaRepository.remove`). Assim, quando outra tabela ainda referencia o registro, a FK do banco impede a exclusão e a API responde 409, sem um módulo consultar o outro. O `flush` é o que faz a FK falhar dentro do caso de uso; sem ele, o erro só apareceria no commit, como 500.

## Lombok

- **Liberado:** `@RequiredArgsConstructor`, `@Getter`, `@Slf4j` e `@Builder` quando fizer sentido, e `@NoArgsConstructor(access = AccessLevel.PROTECTED)` para o construtor que o JPA exige nas entidades.
- **Proibido:** `@Data` e `@Value`. Para dados imutáveis, use `record`. O `lombok.config` faz a compilação falhar se eles forem usados.
- Injeção de dependência **sempre por construtor** (campos `final` + `@RequiredArgsConstructor`), nunca `@Autowired` em campo.
- Entidades JPA: sem `@EqualsAndHashCode` e `@ToString` gerados. O `equals`/`hashCode` pelo id já vem da `BaseEntity`.

## Nulabilidade

Nada aceita `null`, a não ser o que está marcado com `@Nullable` (`org.jspecify.annotations`, do [JSpecify](https://jspecify.dev)). A regra é verificada na compilação pelo [NullAway](https://github.com/uber/NullAway).

- **Vale para todo o código de produção**, sem anotar classe ou pacote: o NullAway está configurado com `AnnotatedPackages=br.com.arktech.workshop`, que cobre o pacote base e todos os subpacotes. Classes e módulos novos já entram na verificação.
- Marque com `@Nullable` só o que de fato pode ser nulo: campo, parâmetro, retorno ou componente de record.

```java
public record ApiFieldError(@Nullable String field, String message) {}
```

- **Variável local não leva `@Nullable`**: o JSpecify não aceita a anotação ali, e o NullAway infere a nulabilidade da variável pelo fluxo do código.
- Na sobrescrita de um método do Spring, repita o `@Nullable` dos parâmetros e do retorno da classe-mãe (ex.: `handleExceptionInternal` no `GlobalExceptionHandler`).
- Entidades JPA: o construtor `protected` sem argumentos não precisa inicializar os campos (o NullAway trata classes `@Entity`/`@MappedSuperclass` como inicializadas pelo JPA), e o `id` da `BaseEntity` é tratado como não nulo. Ele só existe depois do `save`: não o leia antes. Os campos da `TimeStamping` são `@Nullable` pelo mesmo motivo.
- O `./gradlew build` falha quando um valor `@Nullable` é usado sem checagem, ou quando `null` é passado onde não é aceito. Os testes não são verificados.

## Dependências

- Adicione uma dependência **somente na tarefa que precisar dela**, nunca "para usar depois".
- Versões ficam no version catalog (`gradle/libs.versions.toml`). Dependências gerenciadas pelo Spring Boot entram sem versão.

## Testes

- Testes automatizados com **JUnit**.
- `./gradlew build` roda todos os testes e precisa passar antes de abrir o PR.
- **Unitários:** JUnit puro, sem Spring e sem banco. É o padrão para domínio e regras de negócio.
- **Camada `api`:** `@WebMvcTest(controllers = ...)` com `@AutoConfigureMockMvc(addFilters = false)` e `@Import(ApiErrorTestConfig.class)` é o padrão para testar controllers, validação e respostas de erro sem banco e sem os filtros de segurança. O caso de uso entra com `@MockitoBean`. Veja o `AuthControllerTest`.
- **Integração:** anote a classe com `@IntegrationTest` (ou `@IntegrationTest(webEnvironment = RANDOM_PORT)` para chamar via HTTP). A anotação sobe a aplicação inteira contra um PostgreSQL real (Testcontainers), no perfil `test` (sem herdar o `development`) e com uma `JWT_SECRET` de teste, já com `MockMvc` (com a segurança ligada), e marca o teste com a tag JUnit `integration`. O Docker precisa estar rodando.
- **Rotas protegidas nos testes de integração:** gere o token com o `TokenIssuer` (`tokenIssuer.issue(userId, Set.of(Role.ADMIN))`) e envie no header `Authorization: Bearer <token>`. Veja o `EndpointAuthorizationTest`.
- Controllers criados só para teste também declaram o acesso, porque o `EndpointAccessCoverageTest` os percorre junto com os de produção.
- Os testes de integração compartilham o banco e não o limpam entre si: gere CPF, CNPJ, placa e chassi com o `UniqueTestData` (`uniqueCPF()`, `uniquePlate()`...) para um teste não colidir com os dados de outro.
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
