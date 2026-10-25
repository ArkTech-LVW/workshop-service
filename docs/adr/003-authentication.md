# ADR 003: Autenticação e autorização

- **Status:** Proposto (passa a Aceito com a aprovação do grupo no PR)
- **Data:** 2026-10-24
- **Tarefa:** [KAN-18 (T1.3)](https://arktech-lvw.atlassian.net/browse/KAN-18) e sub-tarefas

## Contexto

- A equipe da oficina (ADMIN, ATTENDANT, MECHANIC) usa as APIs administrativas e se identifica com e-mail e senha.
- As senhas precisam ser guardadas de forma que um vazamento do banco não as revele.

## Opções consideradas

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

## Consequências

- Cada verificação de senha aloca cerca de 16 MB por alguns milissegundos. É irrelevante no volume de uma oficina, mas deve ser considerado ao definir um rate limit no login.
