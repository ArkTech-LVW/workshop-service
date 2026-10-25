API de gestão de oficina mecânica: clientes, veículos, ordens de serviço e estoque.

## Autenticação

As rotas protegidas exigem um token JWT no header `Authorization: Bearer <token>`. O token é obtido em `POST /auth/login` e vale 1h. Para testar por aqui, faça o login, clique em **Authorize** e cole o `accessToken`. Rotas públicas aparecem sem o cadeado.

Todas as rotas têm limite de requisições por usuário (ou por IP, sem token), e o login tem um limite mais restrito, por e-mail e IP. Acima do limite, a resposta é 429 com o header `Retry-After`, mesmo com a senha correta.

## Erros

Toda resposta de erro tem o mesmo formato, o `ApiError`, sempre em `application/json`:

| Campo | Descrição |
|---|---|
| `code` | Código estável do erro (ex.: `customer.notFound`). **É por ele que o cliente deve decidir o que fazer**; nunca muda para um erro existente. |
| `message` | Texto em pt-BR para exibir ao usuário. Pode mudar a qualquer momento. |
| `details` | Lista de `{field, message}` com os campos inválidos, só nos erros de entrada (400). Nos demais é `null`. |

### Status por categoria

| Status | Quando | `code` |
|---|---|---|
| 400 | Entrada inválida: validação, tipo errado, parâmetro ausente | `request.invalidFields`, com `details` |
| 400 | Corpo JSON malformado ou ausente | `request.malformedBody` |
| 401 | Login com e-mail ou senha inválidos | `auth.invalidCredentials` |
| 401 | Sem token numa rota protegida | `auth.unauthenticated` |
| 401 | Token inválido ou expirado | `auth.invalidToken` |
| 403 | Token válido, mas sem permissão para a rota | `auth.accessDenied` |
| 404 | Recurso inexistente | definido pelo módulo (ex.: `customer.notFound`) |
| 404 | Rota inexistente | `request.routeNotFound` |
| 405 | Método HTTP não suportado na rota | `request.methodNotSupported` |
| 406 | Formato de resposta pedido no `Accept` não suportado | `request.notAcceptable` |
| 415 | `Content-Type` não suportado | `request.unsupportedMediaType` |
| 409 | Conflito com o estado atual (ex.: CPF já cadastrado) | definido pelo módulo |
| 422 | Regra de negócio violada | definido pelo módulo |
| 429 | Limite de requisições excedido. O header `Retry-After` diz em quantos segundos tentar de novo | `request.tooManyRequests` |
| 500 | Erro inesperado | `server.unexpectedError`, com um código de referência na mensagem |

O código de referência do 500 aparece também no log do servidor: informe-o ao suporte.

### Exemplos

**400, entrada inválida**

```json
{
  "code": "request.invalidFields",
  "message": "Existem campos inválidos na requisição.",
  "details": [
    { "field": "name", "message": "Não pode estar em branco." },
    { "field": "items[0].quantity", "message": "Deve ser maior que zero." }
  ]
}
```

**404, recurso não encontrado**

```json
{ "code": "customer.notFound", "message": "Cliente não encontrado.", "details": null }
```

**409, conflito**

```json
{ "code": "customer.documentAlreadyExists", "message": "Já existe um cliente com esse CPF/CNPJ.", "details": null }
```

**422, regra de negócio**

```json
{
  "code": "serviceOrder.invalidStatusTransition",
  "message": "Não é possível mudar a OS de FINALIZADA para EM_EXECUÇÃO.",
  "details": null
}
```

**500, erro inesperado**

```json
{
  "code": "server.unexpectedError",
  "message": "Ocorreu um erro inesperado. Código de referência: a1b2c3d4.",
  "details": null
}
```
