# ADR 006: Orçamento complementar durante a execução

- **Status:** Proposto (passa a Aceito com a aprovação do grupo no PR)
- **Data:** 2026-10-30
- **Tarefa:** [KAN-75 (T3.13)](https://arktech-lvw.atlassian.net/browse/KAN-75)

## Contexto

- Durante a execução, o mecânico pode encontrar um reparo que não estava no orçamento. A oficina precisa incluir os itens novos e pedir a aprovação do cliente sem perder o que já foi aprovado e feito.
- Até aqui o orçamento era a OS inteira. Os itens só mudavam em `RECEIVED` e `IN_DIAGNOSIS`, a aprovação valia para todos os itens e o `QuoteApprovedEvent` levava todas as peças, que o `inventory` baixava do estoque ([ADR 004](004-domain-events.md)). Uma segunda aprovação baixaria de novo o que já tinha saído.
- O cliente precisa ver o que já foi feito (e não pode recusar), o que já tinha aprovado e o que é novo. Ele pode querer tudo, só o original (quando dá para continuar sem o reparo novo) ou outro orçamento.
- A troca de solução é comum: o atendente remove uma peça aprovada e inclui outra. Nesse caso, o orçamento original sozinho não serve mais.

## Opções consideradas

### Como modelar o ciclo complementar

- **Entidade `Quote` (uma rodada de orçamento com itens e status próprios):** a mais expressiva, mas muda migrations, endpoints, resultados e a visão do cliente de uma vez, para um fluxo que cabe na própria OS.
- **Reaproveitar `AWAITING_APPROVAL`, `QUOTE_APPROVED` e `QUOTE_REJECTED`:** nenhum status novo, mas cada status passaria a ter regras diferentes conforme a OS já tivesse passado pela execução (reabrir para o diagnóstico, cancelar sem devolver peças, a fila de execução da T4.1 misturando duas esperas).
- **Três status novos (`AWAITING_SUPPLEMENTARY_APPROVAL`, `SUPPLEMENTARY_APPROVED`, `SUPPLEMENTARY_REJECTED`):** cada status tem uma regra só. Em troca, as colunas de status precisaram passar de `VARCHAR(20)` para `VARCHAR(40)`.

### Como saber o que já foi aprovado

- **Só uma marca na OS ("há mudança pendente"):** impede finalizar com mudança pendente, mas não diz quais itens são novos. Sem isso, o sistema não remove os itens novos sozinho quando o cliente escolhe só o original, e a proposta não separa os blocos.
- **`approvedAt` em cada item:** nulo enquanto o item espera o cliente. Toda aprovação preenche os pendentes, e a proposta se monta com `approvedAt` e `completedAt`.

### Item aprovado removido e a resposta "só o original"

- **Remoção pendente:** o item aprovado ganha uma marca de remoção e só sai na aprovação, para voltar se o cliente escolher só o original. Mais um estado no item e um quarto bloco na proposta.
- **"Só o original" proibido quando um item aprovado foi removido:** a OS guarda `approvedItemRemoved`, e o envio com `canProceedWithoutChanges = true` é recusado. Para voltar ao que era, o atendente refaz os itens e reenvia.

### Onde calcular as peças a baixar

- **No `serviceorder`, mandando no evento só a diferença:** a OS passaria a guardar o que já foi aprovado de cada peça, um estado que o estoque já tem.
- **No `inventory`, por diferença:** o evento continua levando todas as peças da OS. O `inventory` compara com o líquido já movimentado para a OS (saídas menos devoluções) e baixa ou devolve só a diferença.

### Como registrar a devolução

- **Tipo novo `RETURN`:** explícito no extrato, mas mexe no enum, no CHECK de `type` e em quem lista os movimentos.
- **`IN` com `service_order_id`:** a ligação com a OS já diz que é uma devolução. Só o CHECK do usuário precisou aceitar `IN` sem usuário, porque o cliente aprova sem login.

## Decisão

1. **Três status novos.** Saindo de `IN_PROGRESS` (com alguma mudança desde a última aprovação) ou de `SUPPLEMENTARY_REJECTED` (pode ser idêntico), o `sendQuote` leva a OS para `AWAITING_SUPPLEMENTARY_APPROVAL`. De lá, `approveQuote` leva para `SUPPLEMENTARY_APPROVED` e `rejectQuote` para `SUPPLEMENTARY_REJECTED`. A retomada é manual: `startExecution` também sai de `SUPPLEMENTARY_APPROVED`.
2. **Os endpoints atuais decidem o destino pelo status.** O único endpoint novo é o `approveOriginalOnly`, na API administrativa e na do cliente.
3. **`approvedAt` em cada item.** Só o serviço concluído fica travado. Item aprovado e ainda não feito pode ser removido, e editar é remover e incluir de novo. Item pendente não pode ser concluído, e a OS não finaliza (nem sozinha) com mudança pendente.
4. **A oficina informa no envio se dá para continuar sem as mudanças (`canProceedWithoutChanges`).** É obrigatório no complementar e proibido no orçamento inicial. O valor `true` é recusado quando um item aprovado foi removido desde a última aprovação (`approvedItemRemoved`). A recusa do cliente limpa o valor, e o reenvio informa de novo.
5. **"Só o original" remove os itens pendentes** e só vale quando a oficina permitiu. O histórico registra a resposta com a descrição `serviceOrder.originalOnlyApproved`.
6. **O estoque confere a diferença a cada aprovação.** O `WithdrawStockUseCase` virou `ReconcileServiceOrderStockUseCase`: para cada peça da OS ou já movimentada para ela, em ordem de id, baixa o que falta ou devolve o que sobrou (`IN` com `service_order_id`). O reenvio idêntico não movimenta nada.
7. **O cancelamento vale também em `AWAITING_SUPPLEMENTARY_APPROVAL` e `SUPPLEMENTARY_REJECTED`.** O agregado registra o `ServiceOrderCanceledEvent` em todo cancelamento, e o `inventory` confere a diferença com alvo zero, devolvendo tudo o que saiu para a OS. `IN_PROGRESS` e `SUPPLEMENTARY_APPROVED` continuam sem cancelamento direto.

## Consequências

- Um item só sai da OS depois da execução se ainda não foi feito. Peça já instalada no carro e devolvida no cancelamento é corrigida com um `ADJUSTMENT` manual do estoquista.
- O `QuoteApprovedEvent` passou a significar "quantidade que a OS deve ter de cada peça", e não "peças a baixar". Um ouvinte novo desse evento não pode supor que todas as peças são novas.
- A conferência lê o líquido da OS antes de travar as peças. Duas aprovações simultâneas da mesma OS não acontecem porque a versão da OS (lock otimista) recusa a segunda.
- A métrica de tempo de execução (T4.1) soma os períodos em `IN_PROGRESS`, já que a OS pode entrar em execução várias vezes ([KAN-35](https://arktech-lvw.atlassian.net/browse/KAN-35)).
- Os eventos de notificação do envio, da aprovação e da recusa (KAN-46) também vão disparar no ciclo complementar, e a mensagem para o cliente precisa considerar isso.
