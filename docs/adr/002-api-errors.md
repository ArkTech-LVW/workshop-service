# ADR 002: Padrão de erros da API

- **Status:** Proposto (passa a Aceito com a aprovação do grupo no PR)
- **Data:** 2026-10-21
- **Tarefa:** [KAN-14 (T0.5)](https://arktech-lvw.atlassian.net/browse/KAN-14)

## Contexto

Sem tratamento próprio, a API devolve o erro padrão do Spring Boot (`timestamp`, `status`, `error`, `path`): sem um código estável para o front decidir o comportamento da tela e sem mensagem em pt_BR. Além disso:

- O domínio precisa sinalizar "não encontrado", "conflito" e "regra violada" sem conhecer HTTP, porque não pode importar Spring (`LayerDependencyTest`).
- A validação de entrada precisa listar todos os campos com problema, com o caminho completo (`vehicles[0].plate`), e as mensagens precisam vir do `messages_pt_BR.properties`, como exige o `CONTRIBUTING.md`.
- Um erro inesperado não pode expor detalhes técnicos, mas o suporte precisa chegar à linha exata do log.

## Opções consideradas

### Formato da resposta

| | `ProblemDetail` (RFC 9457) | Formato próprio `{code, message, details}` |
|---|---|---|
| Código estável para o front | Só via `type` (URI) ou propriedade extra | Campo `code` com a própria chave de i18n |
| Lista de campos inválidos | Propriedade extra, fora da RFC | Campo `details` |
| Campos que o front ignoraria | `type`, `title`, `status`, `instance` | Nenhum |
| Suporte nativo no Spring | Sim | Exige um handler próprio (que faríamos de qualquer forma para traduzir as mensagens) |

### Exceções de domínio

- **Enum de erros com o status:** centraliza, mas o domínio passaria a conhecer o status HTTP e todo erro novo mexeria no mesmo arquivo.
- **Uma exceção por caso, cada uma mapeada no handler:** o handler cresce a cada caso de uso.
- **Hierarquia por categoria:** `NotFound`, `Conflict` e `BusinessRule`, com chave e argumentos. O handler conhece só as categorias, e os módulos criam subclasses quando quiserem um nome de negócio.

### Exceções do Spring MVC

- **`@ExceptionHandler` explícito por exceção:** fácil de esquecer uma (405, 406, 415...), que escaparia no formato padrão.
- **Estender `ResponseEntityExceptionHandler`:** cobre todas as exceções do Spring MVC por construção, e cada uma passa por `handleExceptionInternal`, onde o corpo é trocado pelo nosso.

### Mensagens de validação

- **Checagem estática no build** (varrer as anotações e conferir as chaves): pega o erro mais cedo, mas exige ferramenta própria.
- **Interpolação estrita em runtime:** um interpolador que só aceita `{chave}` existente no `MessageSource`. Simples, e o erro aparece no primeiro teste que dispara a validação.

## Decisão

1. **Formato próprio `ApiError{code, message, details}`** para toda resposta de erro, sempre `application/json`. `code` é a chave de i18n, `message` é o texto em pt_BR e `details` é a lista de `{field, message}` nos erros de entrada, ou `null`.
2. **Hierarquia selada em `shared/domain/exception`:** `DomainException` (sealed) com `NotFoundException` → 404, `ConflictException` → 409 e `BusinessRuleException` → 422, todas `non-sealed`. A exceção carrega só a chave e os argumentos, e a tradução acontece na `api`.
3. **`GlobalExceptionHandler` em `shared/api/error`, estendendo `ResponseEntityExceptionHandler`:**
   - Erros de entrada (Bean Validation no corpo e nos parâmetros, tipo inválido, parâmetro ausente, valor JSON inválido) saem como 400 `request.invalidFields` com `details`. JSON sintaticamente quebrado ou corpo ausente sai como 400 `request.malformedBody`.
   - As demais exceções do Spring MVC recebem um código pelo status (`request.routeNotFound`, `request.methodNotSupported` etc.), preservando os headers (ex.: `Allow` no 405).
   - Qualquer outra exceção, e todo 5xx, sai como 500 `server.unexpectedError`, com um código de referência de 8 caracteres na mensagem. O mesmo código vai para o log `ERROR`, junto com o stack trace. Erros 4xx não são logados.
4. **`ApiErrorController` no lugar do `BasicErrorController`:** atende o `/error`, para onde o Tomcat encaminha o que escapa do `DispatcherServlet` (exceção em filtro, exceção dentro do próprio advice).
   - Com exceção: sempre 500 `server.unexpectedError`, com código de referência e log, como no advice.
   - Só com status, sem exceção (ex.: `response.sendError(404)` num filtro): mantém o status e usa o mesmo código por status do advice.
   - Responde sempre em JSON, inclusive para `Accept: text/html`.
   - A montagem do `ApiError` e o log do código de referência ficam no `ApiErrors`, usado pelo advice e pelo `ApiErrorController`.
5. **Validação estrita:** `spring-boot-starter-validation` com um `LocalValidatorFactoryBean` próprio (`shared/infrastructure/validation`), cujo interpolador só aceita mensagens no formato `{chave}` e resolve a chave pelo `MessageResolver`, em pt_BR. Texto literal ou chave inexistente lança exceção na validação, que vira 500 com a chave no log.

## Consequências

- O front decide pelo `code`, nunca pelo texto, e marca os campos pelo `field` de cada item de `details`.
- Toda anotação de validação usada pela primeira vez precisa ter a mensagem cadastrada no `messages_pt_BR.properties`; senão, o teste que a dispara quebra com 500.
- As chaves das `DomainException` só são verificadas em runtime. Uma chave inexistente faz o advice falhar, e a exceção original sai pelo `ApiErrorController` como 500. Cada caso de uso deve ter um teste que dispara o erro.
- **401/403 (resolvido na KAN-49).** As falhas de autenticação e autorização acontecem nos filtros do Spring Security, fora do `DispatcherServlet`, onde o `@RestControllerAdvice` não alcança. O `AuthenticationEntryPoint` e o `AccessDeniedHandler` da cadeia repassam a exceção ao `HandlerExceptionResolver` do MVC, e o `GlobalExceptionHandler` a trata como qualquer outra: 401 `auth.unauthenticated` (sem token) ou `auth.invalidToken` (token inválido ou expirado), com `WWW-Authenticate: Bearer`, e 403 `auth.accessDenied`. Um 401/403 enviado por `sendError` chega ao `ApiErrorController` com os mesmos códigos.
- **Locale nos filtros:** antes do `DispatcherServlet`, o `LocaleContextHolder` tem o locale da requisição (`Accept-Language`), não o fixo. Por isso o `MessageResolver` usa sempre o locale da aplicação (`spring.web.locale`), em vez do `LocaleContextHolder`. Para suportar outros idiomas por requisição no futuro, ele volta a resolver o locale da requisição, e o tratamento nos filtros precisa do mesmo cuidado.
- Requisições que o Tomcat rejeita antes de chegar ao Spring (linha de requisição malformada, header grande demais) não passam pelo `/error` e saem na resposta padrão do Tomcat.
- `spring.mvc.problemdetails.enabled` não pode ser ativado: registraria um segundo advice concorrendo com o nosso.
- A KAN-43 (Swagger) vai documentar `ApiError` e `ApiFieldError`, que por isso são públicos.
