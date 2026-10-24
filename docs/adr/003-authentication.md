# ADR 003: Autenticação e autorização

- **Status:** Proposto (passa a Aceito com a aprovação do grupo no PR)
- **Data:** 2026-10-24
- **Tarefa:** [KAN-18 (T1.3)](https://arktech-lvw.atlassian.net/browse/KAN-18) e sub-tarefas

## Contexto

- A equipe da oficina (ADMIN, ATTENDANT, MECHANIC) usa as APIs administrativas e se identifica com e-mail e senha.
- As senhas precisam ser guardadas de forma que um vazamento do banco não as revele.
- Os casos de uso precisam registrar quem fez cada ação, sem que os outros módulos (ordem de serviço, estoque...) dependam do módulo de usuários.
- Os erros 401 e 403 seguem o `ApiError` ([ADR 002](002-api-errors.md)).

## Opções consideradas

### Sessão ou token

| | Sessão (cookie) | JWT stateless |
|---|---|---|
| Estado no servidor | Sim | Não |
| Revogação imediata | Sim | Só quando o token expira (ou com lista de bloqueio) |
| Front em outro domínio e Swagger | Exige CSRF e cookie cross-site | Header `Authorization` |

### Assinatura do JWT

- **HS256 (chave simétrica):** a própria aplicação emite e valida; basta uma variável de ambiente.
- **RS256 (par de chaves):** só compensa quando outro serviço precisa validar tokens sem poder emiti-los.

### Hash de senha

| | BCrypt | Argon2id |
|---|---|---|
| OWASP | Aceitável | Primeira recomendação |
| Ataque com GPU/ASIC | Só custo de CPU | Custo de memória (16 MB por hash) |
| Tamanho da senha | Limite de 72 bytes | Sem limite prático |
| Dependência | Nenhuma | BouncyCastle (`bcprov`) |

## Decisão

### Senhas

- **Argon2id** via `DelegatingPasswordEncoder`, com os parâmetros do Spring Security 5.8 (16 MB, 2 iterações), atrás da porta `PasswordHasher` do domínio.
- O hash é gravado com o prefixo do algoritmo (`{argon2}...`): trocar de algoritmo no futuro não invalida as senhas existentes.
- O agregado `User` recebe a senha pura e o `PasswordHasher` no construtor, e guarda só o hash. Não existe getter do hash.

### Token

- **JWT stateless (HS256)**, emitido pela porta `TokenIssuer` e validado pelo OAuth2 Resource Server do Spring Security (Nimbus, sem biblioteca extra).
- Claims: `sub` (id do usuário), `roles`, `iat` e `exp`. Validade de 1h (`security.jwt.expiration`).
- A chave vem da variável `JWT_SECRET` (mínimo de 32 bytes). Sem ela, vale uma chave de desenvolvimento definida no `application.yml`, para a aplicação rodar localmente sem configuração.

### Usuário logado e isolamento dos módulos

- O módulo `auth` tem o usuário, o JWT e a cadeia do Spring Security. Os outros módulos só conhecem o que está no `shared`: o enum `Role` e a porta `CurrentUser` (`Long id()`), que o `auth` implementa lendo o `sub` do token.
- Fora do `auth`, a referência a um usuário é sempre `Long userId`.

## Consequências

- Um usuário desativado ou com papéis alterados mantém o token atual até ele expirar (no máximo 1h). Se isso virar problema, as saídas são um token mais curto com refresh token ou consultar o usuário a cada requisição.
- Trocar a `JWT_SECRET` invalida todos os tokens emitidos.
- Um ambiente que suba sem `JWT_SECRET` aceita tokens assinados com a chave de desenvolvimento, que é pública no repositório. Quando houver ambiente de produção, o perfil dele deve exigir a variável, sem valor padrão (`secret: ${JWT_SECRET}`).
- O Spring Security 7 acrescenta a authority `FACTOR_BEARER` às do token. Os papéis são só as authorities que começam com `ROLE_`.
- Cada verificação de senha aloca cerca de 16 MB por alguns milissegundos. É irrelevante no volume de uma oficina, mas deve ser considerado ao definir um rate limit no login.
