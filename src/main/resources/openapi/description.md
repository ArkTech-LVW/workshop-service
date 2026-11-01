API de gestão de oficina mecânica: clientes, veículos, ordens de serviço e estoque.

## Autenticação

As rotas protegidas exigem um token JWT no header `Authorization: Bearer <token>`. O token é obtido em `POST /auth/login` e vale 1h. Para testar por aqui, faça o login, clique em **Authorize** e cole o `accessToken`. Rotas públicas aparecem sem o cadeado.

As rotas do cliente (`/customer/...`) não usam token: o cliente informa o `publicId` da OS na URL e o próprio CPF/CNPJ no header `X-Customer-Document`.

Todas as rotas têm limite de requisições por usuário (ou por IP, sem token), e o login tem um limite mais restrito, por e-mail e IP. Acima do limite, a resposta é 429 com o header `Retry-After`, mesmo com a senha correta.

Cada rota protegida diz, no fim da descrição, quais **papéis** podem chamá-la. Um papel fora da lista recebe 403.

## Fluxo completo

O roteiro abaixo leva uma OS da abertura à entrega usando só esta página. Os exemplos de cada rota já se encaixam (mesmo CPF, mesma placa, ids `1`): em banco limpo, basta clicar em **Try it out** e **Execute**. Em banco com dados, troque o CPF, a placa e o SKU e use os ids devolvidos pelas respostas.

1. **Login.** `POST /auth/login` com o corpo de exemplo (o ADMIN `admin@workshop.local` / `admin123`, que só existe no perfil `development`). Clique em **Authorize** e cole o `accessToken`.
2. **Cliente.** `POST /admin/customers`. Guarde o `id`.
3. **Veículo.** `POST /admin/vehicles` com o `customerId` do passo 2. Opcional: a abertura da OS cadastra o veículo se a placa ainda não existir.
4. **Serviço do catálogo.** `POST /admin/services`. Guarde o `id`.
5. **Peça e estoque.** `POST /admin/parts` e depois `POST /admin/parts/{partId}/stockMovements` com `type` = `IN`. **A peça nasce com estoque zero: sem essa entrada, incluir a peça na OS responde 422.**
6. **Abertura da OS.** `POST /admin/serviceOrders` com o CPF do cliente e a placa. Guarde o `id` e o `publicId`: o `id` é usado nas rotas da oficina, e o `publicId` nas rotas do cliente.
7. **Diagnóstico e orçamento.** Na OS do passo 6:
   1. `POST /admin/serviceOrders/{id}/startDiagnosis` (o usuário logado vira o mecânico responsável);
   2. `POST /admin/serviceOrders/{id}/services` e `POST /admin/serviceOrders/{id}/parts`. Guarde o `itemId` do serviço;
   3. `POST /admin/serviceOrders/{id}/sendQuote` com o exemplo **Orçamento inicial** (corpo `{}`): `canProceedWithoutChanges` só vale no orçamento complementar e, no inicial, responde 422. As peças ficam reservadas por 7 dias.
8. **Aprovação pelo cliente, sem token.** `GET /customer/serviceOrders/{publicId}` com o header `X-Customer-Document` = CPF do cliente, e depois `POST /customer/serviceOrders/{publicId}/approve`. O estoque das peças é baixado aqui.
9. **Execução.** `POST /admin/serviceOrders/{id}/startExecution`, e para cada serviço `POST /admin/serviceOrders/{id}/services/{itemId}/start` e `POST /admin/serviceOrders/{id}/services/{itemId}/complete`. Ao concluir o último serviço, a OS vai sozinha para `COMPLETED`.
10. **Entrega.** `POST /admin/serviceOrders/{id}/deliver`. `GET /admin/serviceOrders/{id}` mostra o histórico de status completo.
11. **Métricas.** `GET /admin/metrics/averageExecutionTime` com `from` e `to` cobrindo a data de hoje.

Caminhos alternativos:

- **Rejeição:** no passo 8, `POST /customer/serviceOrders/{publicId}/reject`. A oficina reabre com `POST /admin/serviceOrders/{id}/reopenQuote`, ajusta os itens e reenvia.
- **Orçamento complementar:** durante a execução, inclua um item e chame `sendQuote` com `canProceedWithoutChanges`. O cliente aprova tudo (`approve`) ou só o original (`approveOriginalOnly`).
- **Cancelamento:** `POST /admin/serviceOrders/{id}/cancel`, enquanto o orçamento não foi aprovado (a descrição da rota lista os status aceitos).
- **Aprovação no balcão:** `POST /admin/serviceOrders/{id}/approve` faz o mesmo que o passo 8, pela oficina.

## Erros

Toda resposta de erro tem o mesmo formato, o `ApiError`, sempre em `application/json`:

| Campo | Descrição |
|---|---|
| `code` | Código estável do erro (ex.: `customer.notFound`). **É por ele que o cliente deve decidir o que fazer**; nunca muda para um erro existente. |
| `message` | Texto em pt-BR para exibir ao usuário. Pode mudar a qualquer momento. |
| `details` | Lista de `{field, message}` com os campos inválidos, só nos erros de entrada (400). Nos demais, o campo não vem na resposta. |

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
{ "code": "customer.notFound", "message": "Cliente 1234 não encontrado." }
```

**409, conflito**

```json
{ "code": "customer.documentAlreadyExists", "message": "Já existe um cliente com o CPF/CNPJ 529.982.247-25." }
```

**422, regra de negócio**

```json
{
  "code": "serviceOrder.invalidStatusTransition",
  "message": "Não é possível mudar a OS de FINALIZADA para EM_EXECUÇÃO."
}
```

**500, erro inesperado**

```json
{
  "code": "server.unexpectedError",
  "message": "Ocorreu um erro inesperado. Código de referência: a1b2c3d4."
}
```
