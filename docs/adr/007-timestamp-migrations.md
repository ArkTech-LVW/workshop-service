# ADR 007: Versionamento de migrations por timestamp e execução fora de ordem

- **Status:** Proposto (passa a Aceito com a aprovação do grupo no PR)
- **Data:** 2026-10-31
- **Tarefa:** [KAN-80](https://arktech-lvw.atlassian.net/browse/KAN-80)
- **Substitui:** a convenção de nome e a regra de renumerar migrations do [ADR 001](001-database.md)

## Contexto

- O [ADR 001](001-database.md) definiu o padrão `V<n>__<description>.sql`, com o próximo número livre na `main`. Dois PRs em paralelo disputam o mesmo número, e quem mergeia por último renumera a sua. Caso real: as V12 dos PRs #53, #54 e #55, renumeradas para V13, V14 e V15 conforme os merges.
- O Flyway valida a ordem: se uma migration de versão menor chega à `main` depois de outra maior já aplicada num banco existente, ele falha com "Detected resolved migration not applied" e a aplicação não sobe.
- Hoje o banco só existe localmente, em cada máquina de desenvolvimento e nos testes. Não há ambiente compartilhado.

## Opções consideradas

| | Sequencial (`V<n>`) | Timestamp (`V<yyyyMMddHHmmss>`) |
|---|---|---|
| Dois PRs em paralelo criam a mesma versão | Sempre que partem da mesma `main` | Só se criarem no mesmo segundo |
| Renumerar antes do merge | Sim | Não |
| Versão menor chegando depois de uma maior | Falha na validação | Aplicada, com `out-of-order` |
| Ordem de execução | Sempre a das versões, porque o que chega fora de ordem não roda | A dos timestamps a cada subida; num banco existente, uma migration que chega depois de outra mais nova já aplicada roda depois dela |

## Decisão

1. **Toda migration usa timestamp como versão:** `V<yyyyMMddHHmmss>__<english_snake_case>.sql`, com o horário em UTC (`date -u +%Y%m%d%H%M%S`). O UTC não depende do fuso de quem cria nem do horário de verão.
2. **As migrations sequenciais existentes foram renomeadas** com o timestamp UTC do commit que criou cada uma. A ordem entre elas não mudou. A única exceção foi a troca entre `add_supplementary_quote` e `allow_service_order_stock_return`, que são independentes.
3. **`spring.flyway.out-of-order: true`:** toda migration ainda não executada é aplicada, mesmo com versão menor que a última aplicada. O histórico registra essa migration como `OUT_OF_ORDER`.
4. **Uma migration não pode depender de outra que vem de um branch paralelo e ainda não está na `main`.** Num banco existente, migrations de branches paralelos podem rodar na ordem em que chegaram à `main`, e não na dos timestamps, então cada migration só pode contar com o que já está na `main`. Essa regra entra na revisão do PR.

Continuam valendo do ADR 001: o DDL transacional (uma migration aplicada fora de ordem que falhe também é revertida inteira), o `ddl-auto=validate` e o esquema vindo só das migrations.

## Consequências

- A renumeração no merge deixa de existir.
- Um banco novo (CI, testes, banco recriado) aplica tudo na ordem dos timestamps. Num banco existente, migrations de branches paralelos podem rodar na ordem em que chegaram à `main`, e não na dos timestamps. Migrations que mexem na mesma tabela ou coluna em branches paralelos precisam de atenção na revisão.
- Renomear as migrations existentes invalida os bancos locais já criados: o Flyway acusa as versões antigas aplicadas que não existem mais. Cada pessoa recria o banco local uma vez (`docker compose down -v`). Isso só foi possível porque não há ambiente compartilhado; depois que houver, uma migration aplicada nunca mais muda de nome.
