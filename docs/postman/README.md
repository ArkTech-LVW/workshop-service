# Postman

A collection do Postman é gerada a partir do OpenAPI que a própria aplicação publica em `/docs/openapi.json`. Não há arquivo de collection no repositório: rota nova aparece no próximo import. O plano gratuito do Postman basta.

## Como usar

### 1. Suba a aplicação

```bash
./gradlew bootRun
```

A aplicação sobe em `http://localhost:8080` no perfil `development`, que cria o ADMIN `admin@workshop.local` com a senha `admin123`.

### 2. Importe o OpenAPI

1. No Postman, clique em **Import** e cole `http://localhost:8080/docs/openapi.json`.
2. Escolha importar como **Postman Collection** e abra **View Import Settings**.
3. Mude **Folder organization** para **Tags**, para as rotas ficarem nas pastas **Autenticação**, **Clientes** etc.
4. Mude **Parameter generation** para **Example**, para os corpos e parâmetros virem com os exemplos do Swagger em vez de `<string>`.
5. Clique em **Import**. A collection **Workshop Service API** aparece em **Collections**, com o `baseUrl` já apontando para `http://localhost:8080`.

Quando surgirem rotas novas, importe de novo e apague a collection antiga.

### 3. Tire a auth do login

Abra **Autenticação → Autentica um usuário**, vá na aba **Authorization** e escolha **No Auth**. Sem isso, o login herda o token da collection e, depois que o token vence, responde 401 até você limpar o token ([KAN-61](https://arktech-lvw.atlassian.net/browse/KAN-61)).

### 4. Faça o login e guarde o token

1. Envie **Autentica um usuário**. O corpo já vem com o ADMIN de desenvolvimento.
2. Copie o `accessToken` da resposta.
3. Clique na collection, vá na aba **Authorization** e cole o token no campo **Token**. Todas as rotas herdam essa auth.

O token vale 1h. Quando as rotas começarem a responder 401, repita o login e troque o token.

### 5. Use as rotas

- **Clientes → Cadastra um cliente** cria um cliente com o CPF `529.982.247-25`. Para cadastrar outro, troque o documento no corpo, porque um CPF repetido responde 409.
- **Veículos → Cadastra um veículo de um cliente** usa o cliente de id `1`, a placa `ABC-1D23` e o chassi `9BWZZZ372VT004251`. Para cadastrar outro, troque a placa e o chassi (ou remova o chassi, que é opcional), porque placa ou chassi repetido responde 409.
- Nas rotas com `:id`, ajuste o id na aba **Params**.
- Cada rota traz, em **Examples**, as respostas documentadas no Swagger (sucesso e erros).

## Como manter

Não há collection para exportar. Para a rota nova sair utilizável no import, ela precisa estar completa no Swagger: `summary` na operação, `@Tag` no controller e `example` em cada campo do corpo e em cada parâmetro (ver "Postman" no [CONTRIBUTING](../../CONTRIBUTING.md)).
