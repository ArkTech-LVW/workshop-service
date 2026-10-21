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

- `domain` **não importa** `api`, `application`, `infrastructure`, Spring (`org.springframework..`) nem Hibernate (`org.hibernate..`). Anotações `jakarta.persistence` são permitidas.
- `application` não importa `api` nem `infrastructure`.
- `api` não importa `infrastructure`.
- Repositórios Spring Data (`JpaRepository` etc.) só existem na `infrastructure`.

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

## Banco de dados e persistência

PostgreSQL 18 com Spring Data JPA e Flyway. Os motivos estão no [ADR 001](docs/adr/001-database.md).

A imagem do banco tem versão fixa (`postgres:18.6-alpine`) e aparece em dois lugares: `docker-compose.yml` (desenvolvimento) e `TestcontainersConfiguration` (testes). Ao atualizar a versão, mude os dois.

### Migrations

- O esquema vem **só das migrations** (`ddl-auto=validate`): toda tabela, coluna ou índice novo entra por uma migration em `src/main/resources/db/migration`.
- Nome no padrão `V<n>__<description>.sql`, com a descrição em inglês e snake_case (ex.: `V2__create_customers.sql`). Use o próximo número livre na `main`; se outro PR mergear a mesma versão antes do seu, renumere a sua.
- **Nunca altere uma migration que já está na `main`.** Para corrigir, crie uma nova.
- Tabelas no plural e colunas em inglês e snake_case (`service_orders`, `created_at`).
- Chave primária: `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`.

### Entidades e repositórios

As entidades de domínio levam as anotações `jakarta.persistence` direto. O repositório é uma interface Java pura no domínio, e a infraestrutura a implementa com Spring Data:

```java
// <module>/domain
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected Customer() {} // exigido pelo JPA

    // O id só existe após o persist: duas entidades novas nunca são iguais
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(Long id);
}

// <module>/infrastructure
interface CustomerJpaRepository extends JpaRepository<Customer, Long>, CustomerRepository {}
```

- Os métodos da interface do domínio seguem as convenções de nome do Spring Data (`save`, `findById`, `findByDocument`...), que gera a implementação. Consultas que não dá para derivar do nome são redeclaradas na interface da infraestrutura com `@Query`.
- A `application` injeta sempre a interface do domínio (`CustomerRepository`), nunca a do Spring Data.
- Transações (`@Transactional`) ficam nos casos de uso da `application`.

## Lombok

- **Liberado:** `@RequiredArgsConstructor`, `@Getter`, `@Slf4j` e `@Builder` quando fizer sentido.
- **Proibido:** `@Data` e `@Value`. Para dados imutáveis, use `record`. O `lombok.config` faz a compilação falhar se eles forem usados.
- Injeção de dependência **sempre por construtor** (campos `final` + `@RequiredArgsConstructor`), nunca `@Autowired` em campo.
- Entidades JPA: sem `@EqualsAndHashCode` e `@ToString` gerados. Escreva `equals`/`hashCode` à mão, baseados no id.

## Dependências

- Adicione uma dependência **somente na tarefa que precisar dela**, nunca "para usar depois".
- Versões ficam no version catalog (`gradle/libs.versions.toml`). Dependências gerenciadas pelo Spring Boot entram sem versão.

## Testes

- Testes automatizados com **JUnit**.
- `./gradlew build` roda todos os testes e precisa passar antes de abrir o PR.
- **Unitários:** JUnit puro, sem Spring e sem banco. É o padrão para domínio e regras de negócio.
- **Integração:** anote a classe com `@IntegrationTest` (ou `@IntegrationTest(webEnvironment = RANDOM_PORT)` para chamar via HTTP). A anotação sobe a aplicação inteira contra um PostgreSQL real (Testcontainers) e marca o teste com a tag JUnit `integration`. O Docker precisa estar rodando.
- Use `@IntegrationTest` só quando o teste precisa da aplicação ou do banco de verdade. Testes com a mesma configuração compartilham contexto e container; cada configuração diferente (ex.: outro `webEnvironment`) sobe os seus.

## Pull Requests

- Todo PR é aberto para a `main` usando o template do repositório, com o link da tarefa no Jira.
- O merge é feito com **merge commit**.
