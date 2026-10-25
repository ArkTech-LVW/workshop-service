# ADR 003: Autenticação e autorização

- **Status:** Proposto (passa a Aceito com a aprovação do grupo no PR)
- **Data:** 2026-10-24
- **Tarefa:** [KAN-18 (T1.3)](https://arktech-lvw.atlassian.net/browse/KAN-18) e sub-tarefas; autorização na [KAN-19 (T1.4)](https://arktech-lvw.atlassian.net/browse/KAN-19)

## Contexto

- A equipe da oficina (ADMIN, ATTENDANT, MECHANIC) usa as APIs administrativas e se identifica com e-mail e senha.
- As senhas precisam ser guardadas de forma que um vazamento do banco não as revele.
- Os casos de uso precisam registrar quem fez cada ação, sem que os outros módulos (ordem de serviço, estoque...) dependam do módulo de usuários.
- Os erros 401 e 403 seguem o `ApiError` ([ADR 002](002-api-errors.md)).
- Um endpoint esquecido sem restrição não pode ficar aberto: o padrão precisa ser negar.

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

### Autorização por endpoint

| | Caminho na `SecurityConfig` | Method security do Spring (`@RolesAllowed`, `@PreAuthorize`) | Anotações próprias lidas na cadeia |
|---|---|---|---|
| Endpoint sem declaração | Cai na regra final | **Passa** (só checa o que está anotado) | Negado |
| Rota pública | Caminho na `SecurityConfig` | Caminho na `SecurityConfig` **e** `@PermitAll` (a anotação roda depois da cadeia) | Só a anotação |
| Papéis | String | String | Enum `Role` |
| Regra perto do endpoint | Não | Sim | Sim |
| Código próprio | Nenhum | Nenhum | Um `AuthorizationManager` |

## Decisão

### Autorização

- **Deny by default.** Cada endpoint declara o acesso com `@PublicEndpoint`, `@Authenticated` ou `@AllowedRoles({Role...})` (`shared/api/security`), no método ou no tipo, inclusive na interface `IXxxController`. A do método vence a do tipo; duas no mesmo elemento são erro de configuração. Sem declaração, ninguém acessa, nem o ADMIN.
- Sem hierarquia de papéis e sem anotações compostas: cada endpoint lista os papéis que aceita.
- A decisão é tomada na cadeia do Spring Security por um `AuthorizationManager` (`EndpointAuthorizationManager`), que encontra o handler da requisição no `RequestMappingHandlerMapping` e lê a anotação (`EndpointAccess`). O resultado fica em cache por método. Rota sem handler é negada; rota existente com a requisição fora do formato (método, `Content-Type`, `Accept`) é liberada para o MVC responder o erro dele (405, 415, 406).
- Fora dos controllers, a `SecurityConfig` libera só o health e a documentação.
- O `EndpointAccessCoverageTest` percorre todos os endpoints registrados e quebra o build quando algum fica sem declaração. O Swagger usa a mesma leitura (`EndpointAccess`) para tirar o cadeado das rotas públicas e incluir o 401/403 nas protegidas, então a documentação não diverge do comportamento.

### Senhas

- **Argon2id** via `DelegatingPasswordEncoder`, com os parâmetros do Spring Security 5.8 (16 MB, 2 iterações), atrás da porta `PasswordHasher` do domínio.
- O hash é gravado com o prefixo do algoritmo (`{argon2}...`): trocar de algoritmo no futuro não invalida as senhas existentes.
- O agregado `User` recebe a senha pura e o `PasswordHasher` no construtor, e guarda só o hash. Não existe getter do hash.

### Token

- **JWT stateless (HS256)**, emitido pela porta `TokenIssuer` e validado pelo OAuth2 Resource Server do Spring Security (Nimbus, sem biblioteca extra).
- Claims: `sub` (id do usuário), `roles`, `iat` e `exp`. Validade de 1h (`security.jwt.expiration`).
- A chave vem da variável `JWT_SECRET` (mínimo de 32 bytes), sem valor padrão no `application.yml`: sem ela, a aplicação não sobe. Só o perfil `development` traz uma chave de desenvolvimento, para rodar localmente sem configuração.
- O perfil `development` continua sendo o padrão do `application.yml`, para IDE, `bootRun` e testes manuais funcionarem sem passar nada. A imagem Docker define `SPRING_PROFILES_ACTIVE=production`, e o `docker-compose.yml` a sobrescreve com `development` para o ambiente local. Os testes de integração rodam no perfil `test`, com uma chave própria.

### Login

- `POST /auth/login` recebe e-mail e senha e devolve o token.
- E-mail inexistente, senha errada e usuário inativo devolvem o mesmo 401 `auth.invalidCredentials`. Quando o e-mail não existe, a senha é comparada com um hash fictício, para o tempo de resposta não revelar se o e-mail está cadastrado.

### ADMIN de desenvolvimento

- No perfil `development`, a aplicação cria na subida um ADMIN pelo `CreateUserUseCase`, se ele ainda não existir. As credenciais ficam no `application-development.yml`.
- Foi preferido a uma migration de seed: passa pelas regras do domínio (normalização do e-mail, papéis, hash da senha) e não depende do esquema das tabelas nem de um hash gerado fora da aplicação.
- Servidores rodam no perfil `production`, padrão da imagem Docker, onde o ADMIN não é criado. Assim, nenhum servidor sobe com um ADMIN de senha conhecida, desde que siga o deploy pela imagem ou passe o perfil (ver "Token").

### Gestão de usuários

Entrou na [KAN-45 (T1.5)](https://arktech-lvw.atlassian.net/browse/KAN-45).

- O ADMIN cadastra, lista, detalha, atualiza (nome e papéis), ativa e desativa usuários em `/admin/users`. Cada usuário troca a própria senha em `PUT /auth/password`, informando a senha atual. A nova precisa ser diferente da atual (422 `user.samePassword`), e um usuário desativado com token ainda válido não troca a senha (422 `user.inactive`).
- Não existe exclusão: o usuário desativado não faz login, mas continua referenciado pelo id no histórico dos outros módulos.
- **Sempre existe pelo menos um ADMIN ativo.** O agregado `User` recusa (422 `user.lastActiveAdmin`) desativar ou tirar o papel ADMIN do último ADMIN ativo. A contagem vem do repositório.
- Para duas requisições simultâneas não removerem os dois últimos ADMINs, os casos de uso que podem remover um ADMIN pegam um advisory lock do PostgreSQL (`pg_advisory_xact_lock`) antes de carregar o usuário e contar os ADMINs (`UserRepository.countActiveAdminsForUpdate`). O lock vale até o fim da transação, e a contagem, feita depois dele, já vê o que a transação anterior gravou. Um `SELECT ... FOR UPDATE` nas linhas dos ADMINs não serviria: a remoção do papel apaga uma linha da `user_role`, que a segunda transação continuaria enxergando.

### Rate limit

Entrou na [KAN-51 (T1.6)](https://arktech-lvw.atlassian.net/browse/KAN-51). Sem limite, dá para testar senhas no login sem parar, e cada tentativa aloca cerca de 16 MB (Argon2id).

| | Na aplicação (Bucket4j em memória) | Proxy ou gateway na frente da API |
|---|---|---|
| Infraestrutura | Nenhuma | Um componente a mais para configurar e manter |
| Limite por e-mail do login | Sim | Não (o proxy não lê o corpo) |
| Limite por usuário logado | Sim (`sub` do token) | Só se o proxy validar o JWT |
| Várias instâncias | Cada uma conta sozinha | Contador único |
| Testes | Junto com os da aplicação | Fora do build |

- **Na aplicação, com Bucket4j** (token bucket com recarga contínua). Cada chave tem um balde num cache Caffeine, que descarta o balde depois de um período sem uso (ele já estaria cheio) e guarda no máximo 100 mil chaves.
- **Sem API gateway.** No tamanho atual do projeto (uma aplicação, o volume de uma oficina), um gateway seria mais um componente para configurar e manter, sem ganho que o justifique, e não faria o limite por e-mail do login. Pode entrar no futuro, se a API passar a ter vários serviços ou precisar de controle de tráfego na borda.
- **Contadores em memória, sem Redis.** Com uma única instância, o contador em memória já é o contador de toda a API. O Redis só passa a ser necessário com mais de uma instância (ver "Consequências"). Até lá, seria uma dependência de infraestrutura a mais sem efeito no limite. A troca fica restrita ao `RateLimit`, porque o Bucket4j tem integração com Redis.
- **Por rota, no `application.yml`, sem anotação.** O `RateLimitFilter` conta por usuário logado (`sub` do token) ou, sem token, por IP. A rota cadastrada em `security.rate-limit.routes` (path, método opcional e valores) usa só o limite dela; as demais usam o geral, de 100 por minuto. Quando mais de uma rota casa, vale a mais específica. O health fica de fora, por causa das sondas.
- Foi preferido a uma anotação no controller (`@RateLimited`): os limites ficam todos num lugar só, mudam por ambiente sem recompilar e os testes conseguem baixá-los.
- **Login:** 20 tentativas por minuto por IP, como rota cadastrada, e 5 por minuto por **e-mail + IP**, contadas no `LoginUseCase` antes de verificar a senha (o filtro não lê o corpo). Acima do limite, a resposta é 429 mesmo com a senha correta, para o atacante não descobrir quando acertou.
- Acima do limite: 429 `request.tooManyRequests` no `ApiError`, com o header `Retry-After` em segundos. A resposta `TooManyRequests` entra sozinha em todas as operações do Swagger, como o 500.
- **A conta nunca é bloqueada**, e o limite do login não é só por e-mail: assim, qualquer um conseguiria impedir um usuário de entrar só errando a senha dele em sequência. Com a chave e-mail + IP, quem tenta de outro IP não é afetado.
- O IP vem da porta `ClientAddress` (`shared/application/security`), no mesmo padrão do `CurrentUser`. Com `server.forward-headers-strategy: native`, o Tomcat só aceita o `X-Forwarded-For` de proxies confiáveis (`server.tomcat.remoteip.internal-proxies`, por padrão os endereços privados). Um cliente direto não falsifica o IP pelo header.
- O filtro roda depois do `BearerTokenAuthenticationFilter`: o usuário já é conhecido, e token inválido já foi recusado com 401 (validar um HS256 é barato).

### Usuário logado e isolamento dos módulos

- O módulo `auth` tem o usuário, o JWT e a cadeia do Spring Security. Os outros módulos só conhecem o que está no `shared`: o enum `Role` e a porta `CurrentUser` (`Long id()`), que o `auth` implementa lendo o `sub` do token.
- Fora do `auth`, a referência a um usuário é sempre `Long userId`.

## Consequências

- Um usuário desativado ou com papéis alterados mantém o token atual até ele expirar (no máximo 1h). Se isso virar problema, as saídas são um token mais curto com refresh token ou consultar o usuário a cada requisição.
- Trocar a `JWT_SECRET` invalida todos os tokens emitidos.
- A troca de senha também não invalida os tokens já emitidos.
- Desativar e alterar papéis passam por um único lock, um de cada vez, mesmo quando o usuário não é ADMIN. Ativar não pega o lock. No volume de uma oficina, essa fila é irrelevante.
- Rodar o jar num servidor sem `SPRING_PROFILES_ACTIVE=production` cai no perfil `development`, que aceita tokens assinados com a chave pública do repositório. O deploy pela imagem Docker evita isso, porque ela já vem em `production`; quem rodar o jar direto precisa passar o perfil.
- Configurações específicas de produção (ex.: desligar o Swagger) entram num `application-production.yml` quando forem necessárias.
- Cada requisição casa a rota duas vezes: no `EndpointAuthorizationManager` e no `DispatcherServlet`. O custo é desprezível perto de decidir antes do controller.
- As anotações de segurança do Spring e do Jakarta (`@PreAuthorize`, `@PermitAll`...) não têm efeito: um endpoint só com elas continua bloqueado.
- Qualquer um, mesmo sem token, descobre se uma rota existe pelo 405 do método errado (ou pelo 415 do `Content-Type` errado). Nenhum handler é executado. Os demais caminhos respondem 401/403 sem diferenciar rota inexistente de rota proibida.
- O Spring Security 7 acrescenta a authority `FACTOR_BEARER` às do token. Os papéis são só as authorities que começam com `ROLE_`.
- Cada verificação de senha aloca cerca de 16 MB por alguns milissegundos. O rate limit do login limita quantas acontecem por IP.
- Os contadores do rate limit ficam em memória: valem por instância e zeram quando a aplicação reinicia. Com mais de uma instância, o limite efetivo é multiplicado pelo número delas. Nesse momento, o contador precisa ir para um armazenamento compartilhado (ex.: Bucket4j com Redis).
- Um ataque distribuído (muitos IPs) contra um mesmo e-mail não tem teto: cada IP tem as próprias 5 tentativas por minuto. Se isso virar problema, a saída é somar um limite global por e-mail bem mais frouxo (ex.: 100 por hora), que só atrapalha o usuário de verdade num ataque desse porte.
- Usuários atrás do mesmo IP de um atacante (mesma rede, NAT) dividem o limite por IP com ele.
- Atrás de um proxy que não use endereço privado, `server.tomcat.remoteip.internal-proxies` precisa apontar para ele. Sem isso, todos os clientes aparecem com o IP do proxy e dividem o mesmo limite.
