# Postman

`workshop-service.postman_collection.json` é a collection **Workshop Service**, com as rotas da API, no formato Collection v2.1 do Postman. Qualquer pessoa importa no próprio Postman, no plano gratuito. O arquivo fica no repositório e muda no mesmo PR das rotas.

## Como usar a collection no Postman

### 1. Importe a collection

1. Instale o app do Postman e entre com a sua conta. O plano gratuito basta.
2. Clique em **Import**, no canto superior esquerdo, e selecione o arquivo `docs/postman/workshop-service.postman_collection.json` do seu clone. Também dá para arrastar o arquivo para a janela.
3. A collection **Workshop Service** aparece em **Collections**, com as pastas **Autenticação** e **Clientes**.

Quando o arquivo mudar no repositório, importe de novo. Se o Postman perguntar o que fazer com a collection que já existe, escolha **Replace**, para trocar a versão antiga pela nova.

### 2. Suba a aplicação

```bash
./gradlew bootRun
```

A aplicação sobe em `http://localhost:8080` no perfil `development`, que cria o ADMIN `admin@workshop.local` com a senha `admin123`.

### 3. Faça o login

1. Abra **Autenticação → Autentica um usuário** e clique em **Send**.
2. A resposta 200 traz o `accessToken`, e o Postman o guarda na variável `accessToken` da collection. A partir daí, todas as rotas mandam `Authorization: Bearer <token>` sozinhas.
3. O token vale 1h. Quando as rotas começarem a responder 401, faça o login de novo.

### 4. Use as rotas

- **Clientes → Cadastra um cliente** cria um cliente com o CPF `529.982.247-25`. Para cadastrar outro, troque o documento no corpo, porque um CPF repetido responde 409.
- Nas rotas com `:id`, ajuste o id na aba **Params**.

O `baseUrl` já vem como `http://localhost:8080` nas variáveis da collection (clique na collection → aba **Variables**). Mude só se a aplicação rodar em outro endereço. Não é preciso selecionar nenhum ambiente.

## Como manter a collection

Toda rota nova entra na collection no mesmo PR, só com o caso de sucesso:

1. No Postman, crie a rota na pasta do recurso (crie a pasta, se o recurso for novo). Use como nome o `summary` da rota no Swagger, deixe a auth como **Inherit auth from parent** e mantenha a ordem em que as rotas fazem sentido (cadastrar antes de consultar, excluir por último).
2. Nas variáveis da collection, apague o valor de `accessToken`, para o token não ir para o arquivo.
3. Clique nos três pontos da collection → **Export**, escolha **Collection v2.1** e salve por cima de `docs/postman/workshop-service.postman_collection.json`.
4. Confira no diff que só entrou o que você mudou e que o `accessToken` continua vazio, e commite junto com a rota.
