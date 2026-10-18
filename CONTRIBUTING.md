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

- `domain` **não importa** `api`, `application`, `infrastructure` nem Spring (`org.springframework..`).
- `application` não importa `api` nem `infrastructure`.
- `api` não importa `infrastructure`.

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

## Pull Requests

- Todo PR é aberto para a `main` usando o template do repositório, com o link da tarefa no Jira.
- O merge é feito com **merge commit**.
