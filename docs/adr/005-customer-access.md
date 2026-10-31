# ADR 005: Acesso do cliente à ordem de serviço

- **Status:** Proposto (passa a Aceito com a aprovação do grupo no PR)
- **Data:** 2026-10-29
- **Tarefa:** [KAN-33 (T3.8)](https://arktech-lvw.atlassian.net/browse/KAN-33)

## Contexto

- O cliente acompanha a OS e decide sobre o orçamento pela API dele (T3.8). Ele não tem login: as contas são só da equipe da oficina ([ADR 003](003-authentication.md)).
- A OS tem um `publicId` (UUID v4, gerado na abertura), que a oficina repassa ao cliente. O `id` sequencial é enumerável e nunca aparece nas rotas do cliente.
- O [ADR 001](001-database.md) deixou um ponto em aberto: "A API do cliente (T3.8) precisa autorizar pelo dono do recurso, nunca pela dificuldade de adivinhar o ID". Dar acesso só pelo `publicId` seria exatamente autorizar pela dificuldade de adivinhar.
- Aprovar o orçamento muda o status e baixa o estoque. Só com o `publicId`, um link encaminhado, salvo numa conversa ou num print bastaria para outra pessoa aprovar ou rejeitar o orçamento.

## Opções consideradas

| | Só o `publicId` | `publicId` + CPF/CNPJ do dono | `publicId` + placa | Login do cliente |
|---|---|---|---|---|
| Quem tem só o link vê e decide a OS | Sim | Não | Não | Não |
| O que o cliente informa | Nada | O próprio documento | A placa | E-mail e senha |
| Segredo do segundo fator | Nenhum | Dado pessoal, não fica à vista | Fica à vista no carro | Senha |
| Cadastro e recuperação de conta | Não | Não | Não | Sim, um fluxo novo inteiro |
| Atende o ADR 001 | Não | Sim | Em parte | Sim |

## Decisão

1. **O cliente se identifica pelo `publicId` da OS e pelo CPF/CNPJ do dono.** As rotas `/customer/serviceOrders/{publicId}` (consulta, `approve` e `reject`) são `@PublicEndpoint`. A OS é buscada pelo par `publicId` + id do cliente dono do documento, que vem do `CustomerDirectory` do `registry` (`CustomerServiceOrderFinder`).
2. **O documento vai no header `X-Customer-Document`**, nunca na URL: assim não fica em log de acesso, histórico do navegador nem proxy. As três rotas o recebem do mesmo jeito, inclusive a consulta, que é um GET sem corpo. Header ausente ou com formato inválido responde 400.
3. **OS inexistente e documento que não é do dono respondem o mesmo 404** (`serviceOrder.notFound`, com o `publicId`). A resposta não revela se o `publicId` existe.
4. **As rotas do cliente não devolvem dados internos:** nada de `id` da OS, ids de cliente, veículo, mecânico ou usuário, nem ids dos itens no catálogo e no estoque. O orçamento traz o nome de cada serviço e peça, buscado nos diretórios do `catalog` e do `inventory`. O veículo traz placa, marca, modelo, ano e cor, buscados no `registry`.
5. **O orçamento só aparece depois de enviado** (a partir de `AWAITING_APPROVAL`). Ele some de novo quando o orçamento é reaberto (a OS volta para `IN_DIAGNOSIS` e os itens podem mudar) e quando a OS é cancelada.
6. **Aprovar e rejeitar reutilizam os casos de uso da T3.6** (`ApproveQuoteUseCase` e `RejectQuoteUseCase`), depois de conferir o acesso. A rota pública ignora o header `Authorization`, então a mudança no histórico e a baixa de estoque ficam sem usuário (`changed_by_user_id` e `created_by_user_id` nulos). É isso que identifica uma decisão do cliente.
7. **As rotas do cliente têm limite próprio:** 20 requisições por minuto por IP em `/customer/serviceOrders/**` (`security.rate-limit.routes`), abaixo das 100 do limite geral.

## Consequências

- O cliente precisa do `publicId`, que a oficina repassa, e do próprio documento. Quem tem só o link não vê nada.
- Quem souber o `publicId` e o documento do dono (ex.: alguém da família) vê e decide a OS. É a mesma confiança de atender por telefone quem informa o CPF.
- Com o `publicId` vazado, dá para testar documentos. O limite por IP deixa isso lento, mas não tem teto num ataque distribuído, como o login ([ADR 003](003-authentication.md#rate-limit)).
- A consulta do cliente busca o documento no `registry` e o veículo no `registry`. Quando há orçamento, busca também os nomes no `catalog` e no `inventory`, numa consulta para cada módulo.
- O nome de cada item é o atual do catálogo e do estoque. Se um serviço ou peça for renomeado depois do envio, o cliente vê o nome novo. O preço continua o copiado na inclusão do item.
- Uma OS cancelada depois do envio não mostra mais o orçamento ao cliente, só o status e o histórico.
- Quando o front do cliente rodar em outro domínio, o header `X-Customer-Document` precisa entrar nos headers permitidos do CORS.
