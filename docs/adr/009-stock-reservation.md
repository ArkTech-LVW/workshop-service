# ADR 009: Reserva de estoque no orçamento

- **Status:** Proposto (passa a Aceito com a aprovação do grupo no PR)
- **Data:** 2026-10-31
- **Tarefa:** [KAN-71 (T2.7)](https://arktech-lvw.atlassian.net/browse/KAN-71)

## Contexto

- Hoje a peça só sai do estoque na aprovação do orçamento. Duas OS podem orçar a mesma peça, e a segunda aprovação falha com `422 part.insufficientStock` na frente do cliente.
- Os itens da OS mudam no caminho: remoção, orçamento complementar e "só o original" ([ADR 006](006-supplementary-quote.md)).
- Um orçamento parado não pode prender a peça para sempre.

## Opções consideradas

| | Só `part.reserved_quantity` | Tabela `stock_reservation` + `part.reserved_quantity` |
|---|---|---|
| Quanto cada OS reservou | Recalculado dos itens, que mudam | Uma linha por OS e peça |
| Expirar | Sem data nem dono | `expires_at` em cada linha |
| Disponível e `lowStock` | Uma subtração na peça | Uma subtração na peça |
| `reserved <= stock` garantido pelo banco | Sim | Sim |

Para expirar, também foi considerada a opção de não usar job: a reserva vencida só deixaria de contar no disponível. Ela foi descartada porque o total na peça ficaria desatualizado, e o CHECK do banco só funciona com o total.

## Decisão

1. **A tabela `stock_reservation` é a fonte da verdade, e `part.reserved_quantity` guarda o total**, com `CHECK (reserved_quantity >= 0 AND reserved_quantity <= stock_quantity)`. É o mesmo par que o `stock_movement` forma com o `stock_quantity`.
2. **Disponível = `stock_quantity - reserved_quantity`.** Toda saída (`OUT` e `ADJUSTMENT`) e toda reserva só tiram do disponível, com o mesmo `422 part.insufficientStock`. O `lowStock` também usa o disponível.
3. **Incluir peça na OS sem disponível é recusado.** A conta é por diferença: o que falta é a quantidade da peça somada nos itens da OS menos o que já saiu do estoque para ela. Assim, no complementar, os itens já aprovados não contam duas vezes. A inclusão só confere e não trava a peça.
4. **A reserva acontece no envio do orçamento** (inicial e complementar), pela mesma diferença. O envio confere de novo, porque outra OS pode ter reservado a peça depois da inclusão.
5. **A validade é de 7 dias**, renovada a cada envio. Um job agendado libera as reservas vencidas.
6. **Vencer não cancela a OS nem muda o status.** A aprovação tenta de novo. Sem estoque, ela é recusada, e a oficina pode retirar o orçamento para trocar a peça.

```
addPartItem ──► confere o disponível (não reserva)
sendQuote ──► confere de novo e reserva por 7 dias
  ├─ aprovação ─────────────────────► reserva vira OUT
  ├─ recusa, cancelamento, retirada ─► reserva liberada, sem movimento
  └─ venceu ────────────────────────► job libera; a OS continua aguardando
        └─ aprovação: com estoque, passa; sem estoque, 422
              └─ a oficina retira, troca a peça e reenvia
```

A entrega é dividida em quatro partes: o saldo na peça (KAN-94), a reserva no envio (KAN-95), a expiração (KAN-96) e a retirada do orçamento (KAN-97).

## Consequências

- A falta de peça aparece na inclusão e no envio, na frente do atendente, e não mais na aprovação, na frente do cliente. A exceção é a reserva que venceu.
- O `insufficientStock` sai da resposta da inclusão de peça, porque a inclusão sem disponível passou a ser recusada.
- O `{1}` do `part.insufficientStock` passa a ser o disponível, e não o estoque.
- Peça com reserva não pode ser excluída: a FK de `stock_reservation` faz a exclusão responder `409 part.inUse`.
